// F2-04/05/08: cash estimate and store plan for a shopping list.
// With save: true it also stores the snapshot taken when the person leaves to shop.

import { loadEstimateInputs, todayInJakarta, userClient } from "../_shared/data.ts";
import { computeEstimate } from "../_shared/estimate.ts";
import { corsHeaders, json, requireUser } from "../_shared/http.ts";

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (!(await requireUser(req))) return json({ error: "Sign in first" }, 401);

  const body = await req.json().catch(() => null);
  const listId: unknown = body?.list_id;
  if (typeof listId !== "string") return json({ error: "list_id is required" }, 400);
  const bufferPct = Math.min(50, Math.max(0, Number(body?.buffer_pct ?? 10) || 0));
  const extraStoreCost = Math.min(50_000, Math.max(0, Number(body?.extra_store_cost ?? 3000) || 0));

  try {
    const db = userClient(req);
    const { items, observations, stores } = await loadEstimateInputs(db, listId);
    const estimate = computeEstimate(items, observations, stores, { bufferPct, extraStoreCost, today: todayInJakarta() });

    let estimateId: string | null = null;
    if (body?.save === true) {
      const saved = await db.from("estimates").insert({
        list_id: listId,
        buffer_pct: bufferPct,
        extra_store_cost: extraStoreCost,
        total: estimate.total,
        low: estimate.low,
        high: estimate.high,
        bring: estimate.bring,
        items_total: estimate.items_total,
        items_missing: estimate.items_missing,
        plan: estimate.plan ?? {},
      }).select("id").single();
      if (saved.error) throw saved.error;
      estimateId = saved.data.id;
      const status = await db.from("shopping_lists").update({ status: "shopping", updated_at: new Date().toISOString() })
        .eq("id", listId);
      if (status.error) throw status.error;
    }

    return json({ ...estimate, extra_store_cost: extraStoreCost, estimate_id: estimateId });
  } catch (err) {
    console.error(err);
    return json({ error: (err as Error).message ?? "Unexpected error" }, 500);
  }
});
