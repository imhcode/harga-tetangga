// F1-11: offline accuracy against hand-checked ground truth.
//
//   node tools/accuracy.ts --init            copy every receipts/out/*.json into receipts/truth/ as a starting point
//   (correct receipts/truth/*.json by hand against the photo: this is the human-in-the-loop step)
//   node tools/accuracy.ts                   compare receipts/out/ (latest probe run) with receipts/truth/
//
// A line counts as correct when kind, quantity, unit price and line total all match the truth.
// Names are reported separately: an exact printed name is what lets an alias match next time.

import { existsSync } from "node:fs";
import { mkdir, readdir, readFile, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { normalizeRawName, unitPriceOf } from "../supabase/functions/_shared/checks.ts";
import type { Receipt, ReceiptLine } from "../supabase/functions/_shared/receipt.ts";

const OUT = join("receipts", "out");
const TRUTH = join("receipts", "truth");

const readReceipt = async (path: string): Promise<Receipt> => {
  const json = JSON.parse(await readFile(path, "utf8"));
  return (json.receipt ?? json) as Receipt;
};

if (process.argv.includes("--init")) {
  await mkdir(TRUTH, { recursive: true });
  for (const file of (await readdir(OUT)).filter((f) => f.endsWith(".json"))) {
    const target = join(TRUTH, file);
    if (existsSync(target)) {
      console.log(`keep   ${target} (already exists)`);
      continue;
    }
    const r = await readReceipt(join(OUT, file));
    const truth = {
      total: r.total,
      lines: r.lines.map(({ kind, raw_name, quantity, unit_price, line_total }) => ({ kind, raw_name, quantity, unit_price, line_total })),
    };
    await writeFile(target, JSON.stringify(truth, null, 2));
    console.log(`create ${target}  <- check every line against the photo`);
  }
  process.exit(0);
}

const sameNumbers = (a: ReceiptLine, b: ReceiptLine) =>
  a.kind === b.kind && a.quantity === b.quantity && unitPriceOf(a) === unitPriceOf(b) && a.line_total === b.line_total;

let lines = 0, numbersOk = 0, namesOk = 0, receipts = 0, totalsOk = 0;
const truthFiles = existsSync(TRUTH) ? (await readdir(TRUTH)).filter((f) => f.endsWith(".json")) : [];
if (truthFiles.length === 0) {
  console.error("No ground truth yet. Run with --init, then correct receipts/truth/*.json by hand.");
  process.exit(1);
}

for (const file of truthFiles) {
  if (!existsSync(join(OUT, file))) {
    console.log(`skip   ${file}: no probe output in ${OUT}`);
    continue;
  }
  const truth = await readReceipt(join(TRUTH, file));
  const read = await readReceipt(join(OUT, file));
  receipts++;
  if (truth.total === read.total) totalsOk++;

  // Walk both lists in order; a missing or extra line costs one truth line.
  let j = 0, ok = 0, names = 0;
  for (const t of truth.lines) {
    const k = read.lines.findIndex((r, idx) => idx >= j && sameNumbers(r, t));
    if (k >= 0) {
      ok++;
      if (normalizeRawName(read.lines[k].raw_name) === normalizeRawName(t.raw_name)) names++;
      j = k + 1;
    }
  }
  lines += truth.lines.length;
  numbersOk += ok;
  namesOk += names;
  console.log(`${file.padEnd(28)} ${ok}/${truth.lines.length} lines correct, ${names} exact names, total ${truth.total === read.total ? "ok" : "WRONG"}`);
}

const pct = (n: number) => (lines ? ((100 * n) / lines).toFixed(1) : "0");
console.log(`\n${receipts} receipts, ${lines} lines: ${pct(numbersOk)}% correct, ${pct(namesOk)}% exact names, ${totalsOk}/${receipts} totals right`);
console.log("Target (PRD): >= 85% lines correct without manual correction.");
