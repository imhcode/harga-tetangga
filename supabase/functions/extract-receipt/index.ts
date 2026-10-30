// F1: receipt photo -> draft for the Review screen.
// Reads the photo (minimal effort, escalating to low when checks fail), runs deterministic checks,
// maps lines to products (confirmed aliases first, then the model), and stores a draft receipt.
// Nothing becomes a price until the person confirms it (confirm_receipt RPC).

import { createClient } from "npm:@supabase/supabase-js@2.45.4";
import { checkReceipt, isPriceJump, type LineFlag, normalizeRawName, unitPriceOf } from "../_shared/checks.ts";
import { corsHeaders, json, modelApiKey, requireUser } from "../_shared/http.ts";
import { type CatalogProduct, matchProducts, type NewProduct } from "../_shared/match.ts";
import { MetaApiError } from "../_shared/meta.ts";
import { extractReceipt, RECEIPT_MODEL, WONDER_MODEL } from "../_shared/receipt.ts";

const ALLOWED_MIME = new Set(["image/jpeg", "image/png", "image/webp"]);
const MAX_BASE64_CHARS = 12_000_000; // about 9 MB of image

// minimal matched low on every value in the 30 Oct probe and is ~2x faster; Wonder is the costly last resort.
const CASCADE = [
  { model: RECEIPT_MODEL, effort: "minimal" },
  { model: RECEIPT_MODEL, effort: "low" },
  { model: WONDER_MODEL, effort: "low" },
];

interface StoreRow {
  id: string;
  name: string;
  branch: string | null;
  kind: string;
}

function findStore(stores: StoreRow[], name: string | null, branch: string | null): StoreRow | undefined {
  if (!name) return undefined;
  const same = stores.filter((s) => s.name.toLowerCase() === name.toLowerCase());
  return same.find((s) => (s.branch ?? "").toLowerCase() === (branch ?? "").toLowerCase()) ?? same[0];
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (!(await requireUser(req))) return json({ error: "Sign in first" }, 401);

  const body = await req.json().catch(() => null);
  const imageBase64: unknown = body?.image_base64;
  const mime: unknown = body?.mime ?? "image/jpeg";
  if (typeof imageBase64 !== "string" || imageBase64.length === 0) return json({ error: "image_base64 is required" }, 400);
  if (typeof mime !== "string" || !ALLOWED_MIME.has(mime)) return json({ error: "Use a JPEG, PNG or WebP photo" }, 400);
  if (imageBase64.length > MAX_BASE64_CHARS) return json({ error: "Photo is too large; resize it first" }, 413);

  const apiKey = modelApiKey();
  const dataUrl = `data:${mime};base64,${imageBase64}`;
  const db = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, {
    global: { headers: { Authorization: req.headers.get("Authorization")! } },
    auth: { persistSession: false },
  });

  try {
    const started = Date.now();

    // 1. Read the photo through the cascade (F4-01): cheap first, stronger only while the numbers
    //    do not add up. The best read wins; every tier tried is kept for cost/accuracy measurement.
    let extraction!: Awaited<ReturnType<typeof extractReceipt>>;
    let check!: ReturnType<typeof checkReceipt>;
    let effort = "";
    const tiers: { model: string; effort: string; ms: number; structural_issues: number; usage: unknown }[] = [];
    for (const tier of CASCADE) {
      const result = await extractReceipt(apiKey, dataUrl, { model: tier.model, reasoningEffort: tier.effort });
      const resultCheck = checkReceipt(result.receipt);
      tiers.push({ model: tier.model, effort: tier.effort, ms: result.latencyMs, structural_issues: resultCheck.structuralIssues, usage: result.usage });
      if (tiers.length === 1 || resultCheck.structuralIssues < check.structuralIssues) {
        extraction = result;
        check = resultCheck;
        effort = tier.effort;
      }
      if (check.structuralIssues === 0) break;
    }
    const escalated = tiers.length > 1;
    const receipt = extraction.receipt;

    // 2. Store, catalog and confirmed aliases (all under the caller's row level security).
    const [storesRes, productsRes] = await Promise.all([
      db.from("stores").select("id,name,branch,kind").order("name"),
      db.from("products").select("id,name").order("name"),
    ]);
    if (storesRes.error) throw storesRes.error;
    if (productsRes.error) throw productsRes.error;
    const stores = storesRes.data as StoreRow[];
    const catalog = productsRes.data as CatalogProduct[];
    const store = findStore(stores, receipt.store_name, receipt.store_branch);

    const items = receipt.lines.map((line, index) => ({ line, index })).filter((x) => x.line.kind === "item");
    const rawNames = [...new Set(items.map((x) => normalizeRawName(x.line.raw_name)))];
    const aliasesRes = rawNames.length
      ? await db.from("product_aliases").select("store_id,raw_name,product_id").in("raw_name", rawNames)
      : { data: [], error: null };
    if (aliasesRes.error) throw aliasesRes.error;
    const aliasFor = (rawName: string): string | undefined => {
      const candidates = (aliasesRes.data ?? []).filter((a) => a.raw_name === normalizeRawName(rawName));
      return (candidates.find((a) => store && a.store_id === store.id) ?? candidates.find((a) => a.store_id === null) ??
        candidates[0])?.product_id;
    };

    // 3. Ask the model only about lines without a confirmed alias.
    const unmatched = items.filter((x) => !aliasFor(x.line.raw_name));
    const matches = await matchProducts(
      apiKey,
      catalog,
      unmatched.map((x) => ({ index: x.index, raw_name: x.line.raw_name })),
      receipt.store_name,
    );

    // 4. Last confirmed price per product, for the price-jump check.
    const productIds = [...new Set(items.map((x) => aliasFor(x.line.raw_name) ?? matches.get(x.index)?.product_id).filter(Boolean))];
    const lastPrice = new Map<string, number>();
    if (productIds.length) {
      const res = await db.from("latest_prices").select("product_id,unit_price,observed_on").in("product_id", productIds)
        .order("observed_on", { ascending: false });
      if (res.error) throw res.error;
      for (const row of res.data ?? []) if (!lastPrice.has(row.product_id)) lastPrice.set(row.product_id, Number(row.unit_price));
    }

    const productName = new Map(catalog.map((p) => [p.id, p.name]));
    const lines = receipt.lines.map((line, index) => {
      const flags: LineFlag[] = [...check.lineFlags[index]];
      const unitPrice = unitPriceOf(line);
      let productId: string | null = null;
      let via: "alias" | "model" | "none" = "none";
      let suggestion: NewProduct | null = null;

      if (line.kind === "item") {
        const alias = aliasFor(line.raw_name);
        const match = matches.get(index);
        if (alias) {
          productId = alias;
          via = "alias";
        } else if (match?.product_id) {
          productId = match.product_id;
          via = "model";
          if (match.confidence !== "high") flags.push("unsure_match");
        }
        suggestion = match?.new_product ?? null;
        if (!productId) flags.push("new_product");
        if (productId && unitPrice != null && isPriceJump(unitPrice, lastPrice.get(productId))) flags.push("price_jump");
      }

      return {
        index,
        kind: line.kind,
        raw_name: line.raw_name,
        quantity: line.quantity,
        unit_price: unitPrice,
        line_total: line.line_total,
        confidence: line.confidence,
        flags,
        product_id: productId,
        product_name: productId ? productName.get(productId) ?? null : null,
        matched_via: via,
        suggestion,
        // New products always need a person to confirm the name once.
        auto_accept: line.kind === "item" && flags.length === 0 && productId !== null,
      };
    });

    const purchaseDate = receipt.purchase_date && /^\d{4}-\d{2}-\d{2}$/.test(receipt.purchase_date)
      ? receipt.purchase_date
      : null;
    const extractMs = Date.now() - started;

    // 5. Save the draft. It becomes prices only through confirm_receipt.
    const draft = await db.from("receipts").insert({
      purchase_date: purchaseDate,
      printed_total: receipt.total,
      extraction: { receipt, usage: extraction.usage, tiers, matches: Object.fromEntries(matches) },
      model: extraction.model,
      effort,
      escalated,
      extract_ms: extractMs,
      // Egress log (F4-05): what this photo looked like when it left the phone.
      sent_bytes: Math.floor(imageBase64.length * 3 / 4),
      sent_width: Number.isInteger(body?.width) ? body.width : null,
      sent_height: Number.isInteger(body?.height) ? body.height : null,
      redactions: Number.isInteger(body?.redactions) ? Math.max(0, body.redactions) : 0,
    }).select("id").single();
    if (draft.error) throw draft.error;

    return json({
      receipt_id: draft.data.id,
      store: store
        ? { id: store.id, name: store.name, branch: store.branch, kind: store.kind }
        : { id: null, name: receipt.store_name ?? "", branch: receipt.store_branch, kind: receipt.store_kind },
      purchase_date: purchaseDate,
      printed_total: receipt.total,
      line_sum: check.lineSum,
      sum_matches: check.sumMatches,
      unreadable: receipt.unreadable,
      effort,
      escalated,
      extract_ms: extractMs,
      lines,
      stores,
      catalog,
    });
  } catch (err) {
    console.error(err);
    if (err instanceof MetaApiError) return json({ error: `Model error ${err.status}` }, 502);
    return json({ error: (err as Error).message ?? "Unexpected error" }, 500);
  }
});
