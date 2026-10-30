// F3-01: after a receipt is saved, compare it with the list the person left with.
// Writes nothing; the app saves the trip after the person checked the result.

import { userClient } from "../_shared/data.ts";
import { corsHeaders, json, requireUser } from "../_shared/http.ts";
import { compareTrip, type PlanQuote, type PlannedItem, type ReceiptLineRow } from "../_shared/trip.ts";

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (!(await requireUser(req))) return json({ error: "Sign in first" }, 401);

  const body = await req.json().catch(() => null);
  const receiptId: unknown = body?.receipt_id;
  if (typeof receiptId !== "string") return json({ error: "receipt_id is required" }, 400);

  try {
    const db = userClient(req);

    const list = await db.from("shopping_lists").select("id").eq("status", "shopping")
      .order("updated_at", { ascending: false }).limit(1).maybeSingle();
    if (list.error) throw list.error;
    if (!list.data) return json({ has_list: false });
    const listId = list.data.id as string;

    const [receipt, observations, items, estimate] = await Promise.all([
      db.from("receipts").select("printed_total,status").eq("id", receiptId).single(),
      db.from("price_observations").select("id,raw_name,product_id,quantity,line_total,unit_price,products(name)")
        .eq("receipt_id", receiptId).order("created_at"),
      db.from("shopping_list_items").select("id,label,product_id").eq("list_id", listId).order("created_at"),
      db.from("estimates").select("id,bring,plan").eq("list_id", listId).order("created_at", { ascending: false })
        .limit(1).maybeSingle(),
    ]);
    for (const res of [receipt, observations, items, estimate]) if (res.error) throw res.error;
    if (receipt.data.status !== "verified") return json({ error: "Receipt is not confirmed yet" }, 409);

    const lines: ReceiptLineRow[] = (observations.data ?? []).map((o) => ({
      observation_id: o.id,
      raw_name: o.raw_name ?? "",
      product_id: o.product_id,
      // deno-lint-ignore no-explicit-any
      product_name: (o as any).products?.name ?? o.raw_name ?? "",
      quantity: o.quantity == null ? null : Number(o.quantity),
      line_total: Number(o.line_total ?? Number(o.unit_price) * Number(o.quantity ?? 1)),
    }));
    const quotes: PlanQuote[] = ((estimate.data?.plan?.items ?? []) as PlanQuote[])
      .map((q) => ({ item_id: q.item_id, unit_price: Number(q.unit_price), quantity: Number(q.quantity) }));
    const comparison = compareTrip(lines, (items.data ?? []) as PlannedItem[], quotes);

    const lineSum = lines.reduce((acc, l) => acc + l.line_total, 0);
    return json({
      has_list: true,
      list_id: listId,
      estimate_id: estimate.data?.id ?? null,
      bring: estimate.data ? Number(estimate.data.bring) : null,
      receipt_total: receipt.data.printed_total != null ? Number(receipt.data.printed_total) : lineSum,
      items: items.data,
      quotes,
      ...comparison,
    });
  } catch (err) {
    console.error(err);
    return json({ error: (err as Error).message ?? "Unexpected error" }, 500);
  }
});
