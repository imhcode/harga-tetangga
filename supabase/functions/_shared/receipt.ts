// Receipt extraction: prompt, JSON schema and the model call.
// Shared by the extract-receipt Edge Function and tools/receipt-probe.ts.

import { chatCompletion } from "./meta.ts";

export const RECEIPT_MODEL = "muse-spark-1.3";

export type Confidence = "high" | "medium" | "low";

export interface ReceiptLine {
  kind: "item" | "discount";
  raw_name: string;
  quantity: number | null;
  unit_price: number | null;
  line_total: number | null;
  source_text: string;
  confidence: Confidence;
}

export type StoreKind = "minimarket" | "supermarket" | "grosir" | "warung" | "lainnya";

export interface Receipt {
  store_name: string | null;
  store_branch: string | null;
  store_kind: StoreKind;
  purchase_date: string | null;
  lines: ReceiptLine[];
  total: number | null;
  unreadable: string[];
}

// Stable part of the prompt goes first so prompt caching can reuse it.
export const RECEIPT_INSTRUCTIONS = `You read Indonesian shopping receipts (minimarket, supermarket, grocery wholesaler, small shop).
Return only what is printed. Never guess.

Rules:
- Money is Indonesian rupiah. Write it as an integer: "12.500" and "12,500" both mean 12500.
- One line per purchased product, in printed order. Use kind "discount" for discount or promo lines
  (POTONGAN, DISC, HEMAT, PROMO) and write their line_total as a negative number.
- raw_name is the product name exactly as printed, including abbreviations and capitals.
- source_text is the full printed text of that line (name, quantity, prices) copied character by character.
- quantity is the number of units bought. For items sold by weight, use the printed weight.
- If a value cannot be read with certainty, use null and add a short note in Indonesian to "unreadable".
- confidence: "high" when every value on the line is clearly legible, "medium" when one value is
  partly unclear, "low" when the name or a price is mostly guessed from context.
- store_name is the chain or shop name only (for example "Indomaret", "Alfamart", "Toko Sumber Rejeki").
  store_branch is the branch name only, without street address, or null.
- store_kind: "minimarket" for convenience chains (Indomaret, Alfamart, Alfamidi), "supermarket",
  "grosir" for wholesalers, "warung" for small neighbourhood shops, "lainnya" for anything else (cafe, restaurant).
- purchase_date uses YYYY-MM-DD. total is the grand total the customer paid, before change and payment lines.
- Ignore payment, change, tax summary, member, card and loyalty lines. Do not copy personal data.`;

const nullableNumber = { type: ["number", "null"] };
const nullableString = { type: ["string", "null"] };

export const RECEIPT_SCHEMA = {
  type: "object",
  additionalProperties: false,
  required: ["store_name", "store_branch", "store_kind", "purchase_date", "lines", "total", "unreadable"],
  properties: {
    store_name: nullableString,
    store_branch: nullableString,
    store_kind: { type: "string", enum: ["minimarket", "supermarket", "grosir", "warung", "lainnya"] },
    purchase_date: nullableString,
    lines: {
      type: "array",
      items: {
        type: "object",
        additionalProperties: false,
        required: ["kind", "raw_name", "quantity", "unit_price", "line_total", "source_text", "confidence"],
        properties: {
          kind: { type: "string", enum: ["item", "discount"] },
          raw_name: { type: "string" },
          quantity: nullableNumber,
          unit_price: nullableNumber,
          line_total: nullableNumber,
          source_text: { type: "string" },
          confidence: { type: "string", enum: ["high", "medium", "low"] },
        },
      },
    },
    total: nullableNumber,
    unreadable: { type: "array", items: { type: "string" } },
  },
};

export interface ExtractResult {
  receipt: Receipt;
  model: string;
  latencyMs: number;
  usage: {
    prompt_tokens: number;
    completion_tokens: number;
    cached_tokens: number;
    reasoning_tokens: number;
  };
}

export async function extractReceipt(
  apiKey: string,
  imageDataUrl: string,
  opts: { model?: string; reasoningEffort?: string } = {},
): Promise<ExtractResult> {
  const started = Date.now();
  const completion = await chatCompletion(apiKey, {
    model: opts.model ?? RECEIPT_MODEL,
    // minimal matched low on every value in the 30 Oct probe and was about 2x faster.
    reasoning_effort: opts.reasoningEffort ?? "minimal",
    // Reasoning tokens count against this limit, so keep it generous or the JSON gets cut off.
    max_completion_tokens: 16000,
    messages: [
      { role: "developer", content: RECEIPT_INSTRUCTIONS },
      {
        role: "user",
        content: [
          { type: "text", text: "Extract this receipt." },
          { type: "image_url", image_url: { url: imageDataUrl } },
        ],
      },
    ],
    response_format: {
      type: "json_schema",
      json_schema: { name: "receipt", strict: true, schema: RECEIPT_SCHEMA },
    },
  });

  const choice = completion.choices[0];
  if (!choice?.message.content) {
    throw new Error(`Empty model response (finish_reason: ${choice?.finish_reason ?? "none"})`);
  }
  if (choice.finish_reason === "length") {
    throw new Error("Model output was cut off; raise max_completion_tokens or lower reasoning_effort");
  }

  return {
    receipt: JSON.parse(choice.message.content) as Receipt,
    model: completion.model,
    latencyMs: Date.now() - started,
    usage: {
      prompt_tokens: completion.usage?.prompt_tokens ?? 0,
      completion_tokens: completion.usage?.completion_tokens ?? 0,
      cached_tokens: completion.usage?.prompt_tokens_details?.cached_tokens ?? 0,
      reasoning_tokens: completion.usage?.completion_tokens_details?.reasoning_tokens ?? 0,
    },
  };
}
