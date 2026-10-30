// F2-02: free-text shopping list -> items, each mapped to the catalog when it clearly is one product.
// The person ticks every item before it joins the list (H4), so a wrong guess costs one tap.

import { chatCompletion } from "./meta.ts";
import type { CatalogProduct } from "./match.ts";
import { type Confidence, RECEIPT_MODEL } from "./receipt.ts";

export interface ParsedItem {
  label: string;
  quantity: number;
  product_id: string | null;
  product_name: string | null;
  confidence: Confidence;
}

const LIST_INSTRUCTIONS = `You turn an Indonesian household shopping list into separate items.
The text may be typed or transcribed speech: commas, new lines, "dan", "sama", or just spaces separate items.

For every item return:
- label: the item as a shopper would write it, in Indonesian, with size or variant if given
  (for example "Beras 5 kg", "Minyak goreng 2 L", "Telur 1 kg", "Indomie goreng").
- quantity: how many units to buy. "2 bungkus indomie" is 2; "beras 5 kg" is 1 (5 kg is the size); default 1.
- product_ref: the catalog reference (like "P3") when exactly one catalog product clearly is this item,
  including brand and size when the list gives them. Use null when the list is generic and several catalog
  products could fit, or when nothing fits.
- confidence: "high" only when the product_ref is certainly right, or when product_ref is null and the
  item itself is clear.

Never invent items that are not in the text.`;

const LIST_SCHEMA = {
  type: "object",
  additionalProperties: false,
  required: ["items"],
  properties: {
    items: {
      type: "array",
      items: {
        type: "object",
        additionalProperties: false,
        required: ["label", "quantity", "product_ref", "confidence"],
        properties: {
          label: { type: "string" },
          quantity: { type: "number" },
          product_ref: { type: ["string", "null"] },
          confidence: { type: "string", enum: ["high", "medium", "low"] },
        },
      },
    },
  },
};

export async function parseShoppingList(apiKey: string, catalog: CatalogProduct[], text: string): Promise<ParsedItem[]> {
  const catalogText = catalog.length ? catalog.map((p, i) => `P${i} | ${p.name}`).join("\n") : "(empty)";
  const completion = await chatCompletion(apiKey, {
    model: RECEIPT_MODEL,
    reasoning_effort: "minimal",
    max_completion_tokens: 8000,
    messages: [
      { role: "developer", content: `${LIST_INSTRUCTIONS}\n\nCatalog:\n${catalogText}` },
      { role: "user", content: text },
    ],
    response_format: { type: "json_schema", json_schema: { name: "shopping_list", strict: true, schema: LIST_SCHEMA } },
  });
  const content = completion.choices[0]?.message.content;
  if (!content) throw new Error("Empty shopping list response");
  const parsed = JSON.parse(content) as { items: { label: string; quantity: number; product_ref: string | null; confidence: Confidence }[] };

  return parsed.items
    .filter((i) => i.label.trim())
    .map((i) => {
      const ref = i.product_ref?.match(/^P(\d+)$/);
      const product = ref ? catalog[Number(ref[1])] : undefined;
      return {
        label: i.label.trim(),
        quantity: i.quantity > 0 ? i.quantity : 1,
        product_id: product?.id ?? null,
        product_name: product?.name ?? null,
        confidence: i.confidence,
      };
    });
}
