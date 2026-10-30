// Deterministic checks on a model-read receipt (F1-05). No model calls here.
// Anything flagged is opened on the Review screen instead of being auto-accepted.

import type { Receipt } from "./receipt.ts";

export type LineFlag =
  | "low_confidence"
  | "missing_price"
  | "qty_price_mismatch"
  | "price_out_of_range"
  | "price_jump"
  | "new_product"
  | "unsure_match";

export interface ReceiptCheck {
  lineSum: number;
  sumMatches: boolean;
  lineFlags: LineFlag[][];
  // Problems with the numbers themselves. Only these trigger a second, slower read;
  // low confidence alone just opens the line on the Review screen.
  structuralIssues: number;
}

const STRUCTURAL: LineFlag[] = ["missing_price", "qty_price_mismatch", "price_out_of_range"];

// Same rule as the SQL in confirm_receipt: upper case, single spaces, trimmed.
export function normalizeRawName(name: string): string {
  return name.trim().replace(/\s+/g, " ").toUpperCase();
}

// Unit price as printed, or derived from line total / quantity when only the total is printed.
export function unitPriceOf(line: Receipt["lines"][number]): number | null {
  if (line.unit_price != null) return line.unit_price;
  if (line.line_total != null && line.quantity) return Math.round(line.line_total / line.quantity);
  return null;
}

export function checkReceipt(receipt: Receipt): ReceiptCheck {
  const lineFlags = receipt.lines.map((line) => {
    const flags: LineFlag[] = [];
    if (line.confidence !== "high") flags.push("low_confidence");
    if (line.kind !== "item") return flags;

    if (line.line_total == null) flags.push("missing_price");
    if (line.quantity != null && line.unit_price != null && line.line_total != null) {
      const tolerance = Math.max(1, Math.abs(line.line_total) * 0.01);
      if (Math.abs(line.quantity * line.unit_price - line.line_total) > tolerance) flags.push("qty_price_mismatch");
    }
    const unit = unitPriceOf(line);
    if (unit != null && (unit < 100 || unit > 5_000_000)) flags.push("price_out_of_range");
    return flags;
  });

  const lineSum = receipt.lines.reduce((acc, l) => acc + (l.line_total ?? 0), 0);
  const sumMatches = receipt.total != null && Math.abs(lineSum - receipt.total) <= 1;
  const structuralIssues = lineFlags.reduce((acc, f) => acc + f.filter((x) => STRUCTURAL.includes(x)).length, 0) +
    (sumMatches ? 0 : 1);
  return { lineSum, sumMatches, lineFlags, structuralIssues };
}

// A price that moved more than 40% from the last confirmed price is worth a second look.
export function isPriceJump(unitPrice: number, lastPrice: number | undefined): boolean {
  return lastPrice != null && lastPrice > 0 && Math.abs(unitPrice - lastPrice) / lastPrice > 0.4;
}
