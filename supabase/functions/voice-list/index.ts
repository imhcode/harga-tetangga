// F4-03: spoken shopping list (WAV, base64) -> transcript -> items to tick. Writes nothing.

import { userClient } from "../_shared/data.ts";
import { corsHeaders, json, modelApiKey, requireUser } from "../_shared/http.ts";
import { parseShoppingList } from "../_shared/list.ts";
import type { CatalogProduct } from "../_shared/match.ts";
import { MetaApiError } from "../_shared/meta.ts";
import { keyWords } from "../_shared/trip.ts";
import { transcribeWav } from "../_shared/voice.ts";

const MAX_BASE64_CHARS = 8_000_000; // about 2 minutes of 24 kHz mono audio

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (!(await requireUser(req))) return json({ error: "Sign in first" }, 401);

  const body = await req.json().catch(() => null);
  const audio: unknown = body?.audio_base64;
  if (typeof audio !== "string" || !audio) return json({ error: "audio_base64 is required" }, 400);
  if (audio.length > MAX_BASE64_CHARS) return json({ error: "Recording is too long" }, 413);

  try {
    const wav = Uint8Array.from(atob(audio), (c) => c.charCodeAt(0));
    const db = userClient(req);
    const products = await db.from("products").select("id,name").order("name");
    if (products.error) throw products.error;
    const catalog = products.data as CatalogProduct[];

    // Brand and product words from the person's own receipts help the recogniser.
    const keywords = [...new Set(catalog.flatMap((p) => keyWords(p.name)))];
    const { transcript, ms } = await transcribeWav(modelApiKey(), wav, keywords);
    if (!transcript) return json({ transcript: "", items: [], transcribe_ms: ms });

    const items = await parseShoppingList(modelApiKey(), catalog, transcript);
    return json({ transcript, items, transcribe_ms: ms });
  } catch (err) {
    console.error(err);
    if (err instanceof MetaApiError) return json({ error: `Model error ${err.status}` }, 502);
    return json({ error: (err as Error).message ?? "Unexpected error" }, 500);
  }
});
