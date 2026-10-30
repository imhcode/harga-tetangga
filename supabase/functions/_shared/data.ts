// Database helpers shared by Edge Functions. Every query runs under the caller's row level security.

import { createClient, type SupabaseClient } from "npm:@supabase/supabase-js@2.45.4";
import type { ListItem, Observation, Store } from "./estimate.ts";

export function userClient(req: Request): SupabaseClient {
  return createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, {
    global: { headers: { Authorization: req.headers.get("Authorization")! } },
    auth: { persistSession: false },
  });
}

export async function loadEstimateInputs(db: SupabaseClient, listId: string) {
  const itemsRes = await db.from("shopping_list_items").select("id,label,product_id,quantity")
    .eq("list_id", listId).order("created_at");
  if (itemsRes.error) throw itemsRes.error;
  const items = (itemsRes.data ?? []).map((i) => ({ ...i, quantity: Number(i.quantity) })) as ListItem[];

  const productIds = [...new Set(items.map((i) => i.product_id).filter((id): id is string => !!id))];
  const [obsRes, storesRes] = await Promise.all([
    productIds.length
      ? db.from("price_observations").select("product_id,store_id,unit_price,observed_on").in("product_id", productIds)
      : Promise.resolve({ data: [], error: null }),
    db.from("stores").select("id,name,branch"),
  ]);
  if (obsRes.error) throw obsRes.error;
  if (storesRes.error) throw storesRes.error;

  return {
    items,
    observations: (obsRes.data ?? []).map((o) => ({ ...o, unit_price: Number(o.unit_price) })) as Observation[],
    stores: (storesRes.data ?? []) as Store[],
  };
}

export function todayInJakarta(): string {
  // Kudus is UTC+7; a receipt from this morning must not look like it came from tomorrow.
  return new Date(Date.now() + 7 * 3_600_000).toISOString().slice(0, 10);
}
