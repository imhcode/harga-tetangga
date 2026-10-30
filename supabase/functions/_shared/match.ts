// Maps printed receipt names to the user's product catalog (F1-06).
// Confirmed aliases are looked up first (in the Edge Function); this model call only sees the rest.
// The catalog is small, so it goes into the prompt whole; a pg_trgm shortlist can replace it when it grows.

import { chatCompletion } from "./meta.ts";
import { RECEIPT_MODEL, type Confidence } from "./receipt.ts";

export const CATEGORIES = [
  "Beras & biji-bijian",
  "Minyak & lemak",
  "Gula & pemanis",
  "Bumbu & kecap",
  "Mi & pasta",
  "Susu & olahan",
  "Telur",
  "Daging & ikan",
  "Sayur & buah",
  "Minuman",
  "Makanan ringan",
  "Roti & kue",
  "Kebersihan rumah",
  "Perawatan diri",
  "Lainnya",
] as const;

const SIZE_UNITS = ["g", "kg", "ml", "l", "pcs", "none"] as const;

export interface NewProduct {
  name: string;
  brand: string | null;
  size_value: number | null;
  size_unit: "g" | "kg" | "ml" | "l" | "pcs" | null;
  category: string;
}

export interface ProductMatch {
  product_id: string | null;
  confidence: Confidence;
  new_product: NewProduct;
}

export interface CatalogProduct {
  id: string;
  name: string;
}

const MATCH_INSTRUCTIONS = `You map product names printed on Indonesian shopping receipts to a household's product catalog.
Printed names are abbreviated and upper case, for example "KNZLER SNGLES KJU 65" or "INDOMI AYAM BWNG 69G".

For every input line return:
- product_ref: the catalog reference (like "P3") of exactly the same product: same brand, same variant, same size.
  Use null when no catalog entry is that exact product. A different size or variant is a different product.
- confidence: "high" only when brand, variant and size all clearly agree with the catalog entry, or, when
  product_ref is null, when you are sure what the printed name means.
- new_product: the product this line describes, written for a shopper in Indonesian title case with the size,
  for example "Kanzler Singles Keju 65 g" or "Indomie Ayam Bawang 69 g". Fill it even when product_ref is set.
  Expand an abbreviation only when you are confident; otherwise keep the printed word.
  brand is the brand or null. size_value and size_unit come from the printed size; use size_unit "none" and
  size_value null when no size is printed. category must be one of the listed categories.

Categories: ${CATEGORIES.join("; ")}.`;

const MATCH_SCHEMA = {
  type: "object",
  additionalProperties: false,
  required: ["matches"],
  properties: {
    matches: {
      type: "array",
      items: {
        type: "object",
        additionalProperties: false,
        required: ["index", "product_ref", "confidence", "new_product"],
        properties: {
          index: { type: "integer" },
          product_ref: { type: ["string", "null"] },
          confidence: { type: "string", enum: ["high", "medium", "low"] },
          new_product: {
            type: "object",
            additionalProperties: false,
            required: ["name", "brand", "size_value", "size_unit", "category"],
            properties: {
              name: { type: "string" },
              brand: { type: ["string", "null"] },
              size_value: { type: ["number", "null"] },
              size_unit: { type: "string", enum: SIZE_UNITS },
              category: { type: "string", enum: CATEGORIES },
            },
          },
        },
      },
    },
  },
};

export async function matchProducts(
  apiKey: string,
  catalog: CatalogProduct[],
  lines: { index: number; raw_name: string }[],
  storeName: string | null,
): Promise<Map<number, ProductMatch>> {
  const result = new Map<number, ProductMatch>();
  if (lines.length === 0) return result;

  // Stable part first (instructions + catalog) so prompt caching can reuse it across receipts.
  const catalogText = catalog.length
    ? catalog.map((p, i) => `P${i} | ${p.name}`).join("\n")
    : "(empty: every line is a new product)";

  const completion = await chatCompletion(apiKey, {
    model: RECEIPT_MODEL,
    reasoning_effort: "minimal",
    max_completion_tokens: 16000,
    messages: [
      { role: "developer", content: `${MATCH_INSTRUCTIONS}\n\nCatalog:\n${catalogText}` },
      { role: "user", content: JSON.stringify({ store: storeName, lines }) },
    ],
    response_format: {
      type: "json_schema",
      json_schema: { name: "product_matches", strict: true, schema: MATCH_SCHEMA },
    },
  });

  const content = completion.choices[0]?.message.content;
  if (!content) throw new Error("Empty product match response");
  const parsed = JSON.parse(content) as {
    matches: { index: number; product_ref: string | null; confidence: Confidence; new_product: NewProduct & { size_unit: string } }[];
  };

  const wanted = new Set(lines.map((l) => l.index));
  for (const m of parsed.matches) {
    if (!wanted.has(m.index)) continue;
    // Only accept references that exist in the catalog we sent; anything else becomes a new product.
    const ref = m.product_ref?.match(/^P(\d+)$/);
    const product = ref ? catalog[Number(ref[1])] : undefined;
    result.set(m.index, {
      product_id: product?.id ?? null,
      confidence: m.confidence,
      new_product: {
        ...m.new_product,
        size_unit: m.new_product.size_unit === "none" ? null : (m.new_product.size_unit as NewProduct["size_unit"]),
      },
    });
  }
  return result;
}
