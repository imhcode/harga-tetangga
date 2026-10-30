// F2-06: shopping agent endpoint. Tools read the caller's own data under row level security.

import { type ChatTurn, runAgent, type ToolExecutors } from "../_shared/agent.ts";
import { loadEstimateInputs, todayInJakarta, userClient } from "../_shared/data.ts";
import { computeEstimate } from "../_shared/estimate.ts";
import { corsHeaders, json, modelApiKey, requireUser } from "../_shared/http.ts";
import { MetaApiError } from "../_shared/meta.ts";

const DAY_MS = 86_400_000;

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (!(await requireUser(req))) return json({ error: "Sign in first" }, 401);

  const body = await req.json().catch(() => null);
  const listId: unknown = body?.list_id;
  const message: unknown = body?.message;
  if (typeof listId !== "string") return json({ error: "list_id is required" }, 400);
  if (typeof message !== "string" || !message.trim()) return json({ error: "message is required" }, 400);
  const history: ChatTurn[] = Array.isArray(body?.history)
    ? body.history
      .filter((t: ChatTurn) => (t?.role === "user" || t?.role === "assistant") && typeof t.content === "string")
      .map((t: ChatTurn) => ({ role: t.role, content: t.content.slice(0, 2000) }))
    : [];

  const db = userClient(req);
  const today = todayInJakarta();

  const estimateFor = async (bufferPct: number) => {
    const { items, observations, stores } = await loadEstimateInputs(db, listId);
    return computeEstimate(items, observations, stores, { bufferPct, extraStoreCost: 3000, today });
  };

  const tools: ToolExecutors = {
    async cari_harga({ nama }) {
      // Letters and digits only: the words go into a PostgREST filter.
      const words = nama.toLowerCase().split(/[^a-z0-9]+/).filter((w) => w.length >= 3).slice(0, 4);
      if (words.length === 0) return { produk: [], catatan: "nama terlalu pendek" };
      const products = await db.from("products").select("id,name").or(words.map((w) => `name.ilike.*${w}*`).join(","));
      if (products.error) throw products.error;
      const ranked = (products.data ?? [])
        .map((p) => ({ ...p, score: words.filter((w) => p.name.toLowerCase().includes(w)).length }))
        .sort((a, b) => b.score - a.score)
        .slice(0, 5);
      if (ranked.length === 0) return { produk: [], catatan: "belum ada data harga" };
      const prices = await db.from("latest_prices").select("product_id,store_name,store_branch,unit_price,observed_on")
        .in("product_id", ranked.map((p) => p.id));
      if (prices.error) throw prices.error;
      return {
        hari_ini: today,
        produk: ranked.map((p) => ({
          nama: p.name,
          harga: (prices.data ?? []).filter((r) => r.product_id === p.id)
            .sort((a, b) => Number(a.unit_price) - Number(b.unit_price))
            .map((r) => ({
              toko: [r.store_name, r.store_branch].filter(Boolean).join(" "),
              harga: Number(r.unit_price),
              tanggal: r.observed_on,
              umur_hari: Math.round((Date.parse(today) - Date.parse(r.observed_on)) / DAY_MS),
            })),
        })),
      };
    },

    async hitung_estimasi({ persen_cadangan }) {
      const e = await estimateFor(persen_cadangan ?? 10);
      if (e.items_total === 0) return { catatan: "daftar belanja masih kosong" };
      return {
        uang_dibawa: e.bring,
        total_perkiraan: e.total,
        rentang: [e.low, e.high],
        cadangan_persen: e.buffer_pct,
        toko: e.plan?.store_names ?? [],
        biaya_tambahan_toko: e.plan?.extra_store_cost ?? 0,
        barang: e.plan?.items.map((q) => ({ barang: q.label, jumlah: q.quantity, toko: q.store_name, harga: q.unit_price, umur_hari: q.age_days })) ?? [],
        belum_ada_data_harga: e.plan?.missing.map((m) => m.label) ?? [],
        jumlah_barang: e.items_total,
      };
    },

    async bandingkan_toko() {
      const e = await estimateFor(10);
      if (e.items_total === 0) return { catatan: "daftar belanja masih kosong" };
      return {
        jumlah_barang_di_daftar: e.items_total,
        biaya_tambahan_per_toko: 3000,
        rencana_terbaik: e.plan
          ? { toko: e.plan.store_names, barang_terhitung: e.plan.items.length, biaya_barang: e.plan.goods, biaya_tambahan: e.plan.extra_store_cost }
          : null,
        pilihan: e.alternatives.map((a) => ({ toko: a.store_names, barang_terhitung: a.items_covered, biaya_barang: a.goods, biaya_tambahan: a.extra_store_cost })),
        hemat: e.savings ? { jumlah: e.savings.amount, dibanding: e.savings.versus_store } : null,
      };
    },
  };

  try {
    const started = Date.now();
    const result = await runAgent(modelApiKey(), history, message.slice(0, 1000), tools);
    return json({ ...result, agent_ms: Date.now() - started });
  } catch (err) {
    console.error(err);
    if (err instanceof MetaApiError) return json({ error: `Model error ${err.status}` }, 502);
    return json({ error: (err as Error).message ?? "Unexpected error" }, 500);
  }
});
