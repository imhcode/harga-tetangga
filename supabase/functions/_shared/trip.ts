// F3-01: compare what was planned (list + estimate snapshot) with what the receipt says was bought.
// Deterministic. The "outside the plan" marks are a proposal the person corrects before saving.

export interface ReceiptLineRow {
  observation_id: string;
  raw_name: string;
  product_id: string;
  product_name: string;
  quantity: number | null;
  line_total: number;
}

export interface PlannedItem {
  id: string;
  label: string;
  product_id: string | null;
}

export interface PlanQuote {
  item_id: string;
  unit_price: number;
  quantity: number;
}

export interface TripLine extends ReceiptLineRow {
  list_item_id: string | null;
  list_item_label: string | null;
  off_plan: boolean;
}

export interface TripComparison {
  lines: TripLine[];
  not_bought: { item_id: string; label: string }[];
  planned_estimated: number;
  planned_actual: number;
  items_off_plan: number;
  off_plan_total: number;
}

const UNIT_WORDS = new Set(["kg", "gr", "gram", "ml", "liter", "ltr", "pcs", "pack", "isi", "bungkus", "botol", "sachet", "dus"]);

// Words that identify an item: letters only, at least 3 long, without sizes and units.
export function keyWords(text: string): string[] {
  return text.toLowerCase().split(/[^a-z]+/).filter((w) => w.length >= 3 && !UNIT_WORDS.has(w));
}

// Same product, or the list label's first key word appears in the product or printed name
// ("Beras 5 kg" matches "Beras Raja Platinum 5 kg"; "Minyak goreng" does not match "Indomie Goreng").
// When several labels qualify, the one sharing the most key words wins ("Indomie Ayam Bawang" over "Mie").
// Misses are fixed by the person on the Trip Result screen.
export function findListItem(line: ReceiptLineRow, items: PlannedItem[]): PlannedItem | null {
  const exact = items.find((i) => i.product_id && i.product_id === line.product_id);
  if (exact) return exact;
  const haystack = `${line.product_name} ${line.raw_name}`.toLowerCase();
  let best: PlannedItem | null = null;
  let bestScore = 0;
  for (const item of items) {
    const words = keyWords(item.label);
    if (words.length === 0 || !haystack.includes(words[0])) continue;
    const score = words.filter((w) => haystack.includes(w)).length;
    if (score > bestScore) {
      best = item;
      bestScore = score;
    }
  }
  return best;
}

export function compareTrip(lines: ReceiptLineRow[], items: PlannedItem[], quotes: PlanQuote[]): TripComparison {
  const tripLines: TripLine[] = lines.map((line) => {
    const item = findListItem(line, items);
    return { ...line, list_item_id: item?.id ?? null, list_item_label: item?.label ?? null, off_plan: item === null };
  });
  return summarize(tripLines, items, quotes);
}

// Recomputed after the person changes any "outside the plan" mark.
export function summarize(lines: TripLine[], items: PlannedItem[], quotes: PlanQuote[]): TripComparison {
  const bought = new Set(lines.filter((l) => !l.off_plan && l.list_item_id).map((l) => l.list_item_id));
  const quoteFor = new Map(quotes.map((q) => [q.item_id, q]));

  let plannedEstimated = 0;
  let plannedActual = 0;
  for (const itemId of bought) {
    const quote = quoteFor.get(itemId!);
    if (!quote) continue; // no price in the estimate: nothing to compare against
    plannedEstimated += quote.unit_price * quote.quantity;
    plannedActual += lines.filter((l) => !l.off_plan && l.list_item_id === itemId).reduce((acc, l) => acc + l.line_total, 0);
  }
  const offPlan = lines.filter((l) => l.off_plan);
  return {
    lines,
    not_bought: items.filter((i) => !bought.has(i.id)).map((i) => ({ item_id: i.id, label: i.label })),
    planned_estimated: plannedEstimated,
    planned_actual: plannedActual,
    items_off_plan: offPlan.length,
    off_plan_total: offPlan.reduce((acc, l) => acc + l.line_total, 0),
  };
}
