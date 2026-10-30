// F2-02: typed (later spoken) shopping list -> items to tick. Writes nothing.

import { userClient } from "../_shared/data.ts";
import { corsHeaders, json, modelApiKey, requireUser } from "../_shared/http.ts";
import { parseShoppingList } from "../_shared/list.ts";
import type { CatalogProduct } from "../_shared/match.ts";
import { MetaApiError } from "../_shared/meta.ts";

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (!(await requireUser(req))) return json({ error: "Sign in first" }, 401);

  const body = await req.json().catch(() => null);
  const text: unknown = body?.text;
  if (typeof text !== "string" || !text.trim()) return json({ error: "text is required" }, 400);
  if (text.length > 4000) return json({ error: "List is too long" }, 413);

  try {
    const db = userClient(req);
    const products = await db.from("products").select("id,name").order("name");
    if (products.error) throw products.error;
    const catalog = products.data as CatalogProduct[];
    const started = Date.now();
    const items = await parseShoppingList(modelApiKey(), catalog, text);
    return json({ items, catalog, parse_ms: Date.now() - started });
  } catch (err) {
    console.error(err);
    if (err instanceof MetaApiError) return json({ error: `Model error ${err.status}` }, 502);
    return json({ error: (err as Error).message ?? "Unexpected error" }, 500);
  }
});
