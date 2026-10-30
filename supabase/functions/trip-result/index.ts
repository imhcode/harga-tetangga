// F3-01: after a receipt is saved, compare it with the list the person left with.
// Writes nothing; the app saves the trip after the person checked the result.

import { userClient } from "../_shared/data.ts";
import { corsHeaders, json, requireUser } from "../_shared/http.ts";
import { compareTrip, type PlanQuote, type PlannedItem, type ReceiptLineRow } from "../_shared/trip.ts";

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (!(await requireUser(req))) return json({ error: "Sign in first" }, 401);

  // Without receipt_id: find the newest confirmed receipt saved after leaving that has no trip yet,
  // so the comparison survives the app being closed between saving and comparing.
  const body = await req.json().catch(() => null);
  let receiptId: unknown = body?.receipt_id;
  if (receiptId !== undefined && typeof receiptId !== "string") return json({ error: "receipt_id must be a string" }, 400);

  try {
    const db = userClient(req);

    const list = await db.from("shopping_lists").select("id").eq("status", "shopping")
      .order("updated_at", { ascending: false }).limit(1).maybeSingle();
    if (list.error) throw list.error;
    if (!list.data) return json({ has_list: false });
    const listId = list.data.id as string;

    const estimate = await db.from("estimates").select("id,bring,plan,created_at").eq("list_id", listId)
      .order("created_at", { ascending: false }).limit(1).maybeSingle();
    if (estimate.error) throw estimate.error;

    if (receiptId === undefined) {
      const [candidates, trips] = await Promise.all([
        db.from("receipts").select("id").eq("status", "verified")
          .gte("verified_at", estimate.data?.created_at ?? "1970-01-01").order("verified_at", { ascending: false }).limit(5),
        db.from("trips").select("receipt_id"),
      ]);
      if (candidates.error) throw candidates.error;
      if (trips.error) throw trips.error;
      const done = new Set((trips.data ?? []).map((t) => t.receipt_id));
      receiptId = (candidates.data ?? []).find((r) => !done.has(r.id))?.id;
      if (!receiptId) return json({ has_list: false });
    }

    const [receipt, observations, items] = await Promise.all([
      db.from("receipts").select("printed_total,status").eq("id", receiptId as string).single(),
      db.from("price_observations").select("id,raw_name,product_id,quantity,line_total,unit_price,products(name)")
        .eq("receipt_id", receiptId as string).order("created_at"),
      db.from("shopping_list_items").select("id,label,product_id").eq("list_id", listId).order("created_at"),
    ]);
    for (const res of [receipt, observations, items]) if (res.error) throw res.error;
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
      receipt_id: receiptId,
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
