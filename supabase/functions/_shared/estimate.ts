// F2-04/05: deterministic estimate and store plan. No model calls here.
// The Estimate screen and the agent's tools both use this, so the agent can only repeat numbers it computed.

export interface ListItem {
  id: string;
  label: string;
  product_id: string | null;
  quantity: number;
}

export interface Observation {
  product_id: string;
  store_id: string;
  unit_price: number;
  observed_on: string; // YYYY-MM-DD
}

export interface Store {
  id: string;
  name: string;
  branch: string | null;
}

export interface EstimateOptions {
  bufferPct: number; // extra cash on top of the high estimate
  extraStoreCost: number; // flat cost of each store after the first (fuel, parking, time)
  today: string; // YYYY-MM-DD
}

export interface ItemQuote {
  item_id: string;
  label: string;
  quantity: number;
  store_id: string;
  store_name: string;
  unit_price: number;
  low: number; // per unit
  high: number; // per unit
  observed_on: string;
  age_days: number;
}

export interface MissingItem {
  item_id: string;
  label: string;
  reason: "no_product" | "no_price_in_plan";
}

export interface Plan {
  store_ids: string[];
  store_names: string[];
  goods: number; // sum of latest prices x quantity
  extra_store_cost: number;
  cost: number; // goods + extra_store_cost, used to compare plans
  items: ItemQuote[];
  missing: MissingItem[];
}

export interface PlanSummary {
  store_names: string[];
  items_covered: number;
  goods: number;
  extra_store_cost: number;
  cost: number;
}

export interface Estimate {
  plan: Plan | null;
  best_single: Plan | null;
  // Best plans first: every single store, then the best two-store plans.
  alternatives: PlanSummary[];
  total: number;
  low: number;
  high: number;
  bring: number;
  buffer_pct: number;
  items_total: number;
  items_missing: number;
  // Only when the best plan is a single store and another single store covers the same items.
  savings: { amount: number; versus_store: string } | null;
}

const DAY_MS = 86_400_000;
const HISTORY_DAYS = 90;

function daysBetween(from: string, to: string): number {
  return Math.max(0, Math.round((Date.parse(to) - Date.parse(from)) / DAY_MS));
}

export function storeLabel(s: Store): string {
  return s.branch ? `${s.name} ${s.branch}` : s.name;
}

// Rupiah cash is handed over in Rp 5.000 steps comfortably.
export function roundUpCash(amount: number, step = 5000): number {
  return Math.ceil(amount / step) * step;
}

export function computeEstimate(
  items: ListItem[],
  observations: Observation[],
  stores: Store[],
  opts: EstimateOptions,
): Estimate {
  const storeById = new Map(stores.map((s) => [s.id, s]));

  // Latest price and recent price range per product and store.
  const latest = new Map<string, Observation>();
  const history = new Map<string, number[]>();
  for (const o of observations) {
    const key = `${o.product_id}|${o.store_id}`;
    const current = latest.get(key);
    if (!current || o.observed_on > current.observed_on) latest.set(key, o);
    if (daysBetween(o.observed_on, opts.today) <= HISTORY_DAYS) {
      history.set(key, [...(history.get(key) ?? []), Number(o.unit_price)]);
    }
  }

  const quote = (item: ListItem, storeId: string): ItemQuote | null => {
    if (!item.product_id) return null;
    const key = `${item.product_id}|${storeId}`;
    const obs = latest.get(key);
    if (!obs) return null;
    const price = Number(obs.unit_price);
    const seen = history.get(key) ?? [price];
    const age = daysBetween(obs.observed_on, opts.today);
    // Older prices are less certain: widen the upper bound.
    const staleness = age > 30 ? 1.1 : age > 14 ? 1.05 : 1;
    return {
      item_id: item.id,
      label: item.label,
      quantity: item.quantity,
      store_id: storeId,
      store_name: storeLabel(storeById.get(storeId) ?? { id: storeId, name: "?", branch: null }),
      unit_price: price,
      low: Math.min(price, ...seen),
      high: Math.round(Math.max(price, ...seen) * staleness),
      observed_on: obs.observed_on,
      age_days: age,
    };
  };

  const buildPlan = (storeIds: string[]): Plan => {
    const chosen: ItemQuote[] = [];
    const missing: MissingItem[] = [];
    for (const item of items) {
      if (!item.product_id) {
        missing.push({ item_id: item.id, label: item.label, reason: "no_product" });
        continue;
      }
      const options = storeIds.map((s) => quote(item, s)).filter((q): q is ItemQuote => q !== null);
      if (options.length === 0) {
        missing.push({ item_id: item.id, label: item.label, reason: "no_price_in_plan" });
        continue;
      }
      // Cheapest wins; on a tie prefer the fresher price.
      options.sort((a, b) => a.unit_price - b.unit_price || a.age_days - b.age_days);
      chosen.push(options[0]);
    }
    const used = [...new Set(chosen.map((q) => q.store_id))];
    const goods = chosen.reduce((acc, q) => acc + q.unit_price * q.quantity, 0);
    const extra = Math.max(0, used.length - 1) * opts.extraStoreCost;
    return {
      store_ids: used,
      store_names: used.map((id) => storeLabel(storeById.get(id)!)),
      goods,
      extra_store_cost: extra,
      cost: goods + extra,
      items: chosen,
      missing,
    };
  };

  // Only stores that price at least one item on the list are worth visiting.
  const candidates = stores
    .map((s) => s.id)
    .filter((id) => items.some((i) => i.product_id && latest.has(`${i.product_id}|${id}`)));

  const singles = candidates.map((id) => buildPlan([id]));
  const pairs: Plan[] = [];
  for (let a = 0; a < candidates.length; a++) {
    for (let b = a + 1; b < candidates.length; b++) pairs.push(buildPlan([candidates[a], candidates[b]]));
  }

  // More items covered beats a lower price: the person needs the items.
  const better = (x: Plan, y: Plan) => y.items.length - x.items.length || x.cost - y.cost;
  const bestSingle = [...singles].sort(better)[0] ?? null;
  const plan = [...singles, ...pairs].sort(better)[0] ?? null;

  const total = plan?.goods ?? 0;
  const low = plan?.items.reduce((acc, q) => acc + q.low * q.quantity, 0) ?? 0;
  const high = plan?.items.reduce((acc, q) => acc + q.high * q.quantity, 0) ?? 0;

  let savings: Estimate["savings"] = null;
  if (plan && plan.store_ids.length === 1) {
    const comparable = singles.filter((s) => s.items.length === plan.items.length && s.store_ids[0] !== plan.store_ids[0]);
    const priciest = comparable.sort((a, b) => b.cost - a.cost)[0];
    if (priciest && priciest.cost > plan.cost) {
      savings = { amount: priciest.cost - plan.cost, versus_store: priciest.store_names[0] };
    }
  }

  const summarize = (p: Plan): PlanSummary => ({
    store_names: p.store_names,
    items_covered: p.items.length,
    goods: p.goods,
    extra_store_cost: p.extra_store_cost,
    cost: p.cost,
  });
  // Pairs whose items all came from one store duplicate a single-store plan.
  const realPairs = pairs.filter((p) => p.store_ids.length === 2).sort(better).slice(0, 3);

  return {
    plan,
    best_single: bestSingle,
    alternatives: [...[...singles].sort(better), ...realPairs].map(summarize),
    total,
    low,
    high,
    bring: plan ? roundUpCash(high * (1 + opts.bufferPct / 100)) : 0,
    buffer_pct: opts.bufferPct,
    items_total: items.length,
    items_missing: plan ? plan.missing.length : items.length,
    savings,
  };
}
