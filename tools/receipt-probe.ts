// F0-03: runs receipt extraction on local photos and reports what came back.
// Usage: node --env-file=.env tools/receipt-probe.ts receipts/a.jpg receipts/b.jpg [--effort low]
// Output JSON goes to receipts/out/ (gitignored: real receipts never enter the repo).

import { mkdir, readFile, writeFile } from "node:fs/promises";
import { basename, extname, join } from "node:path";
import { extractReceipt } from "../supabase/functions/_shared/receipt.ts";

const apiKey = process.env.MODEL_API_KEY;
if (!apiKey) {
  console.error("MODEL_API_KEY is not set. Copy .env.example to .env and fill it in.");
  process.exit(1);
}

const args = process.argv.slice(2);
const effortIdx = args.indexOf("--effort");
const effort = effortIdx >= 0 ? args.splice(effortIdx, 2)[1] : "low";
if (args.length === 0) {
  console.error("Pass one or more receipt photos (jpg, png, webp).");
  process.exit(1);
}

const MIME: Record<string, string> = { ".jpg": "image/jpeg", ".jpeg": "image/jpeg", ".png": "image/png", ".webp": "image/webp" };
const outDir = join("receipts", "out");
await mkdir(outDir, { recursive: true });

const rupiah = (n: number | null) => (n == null ? "?" : `Rp ${n.toLocaleString("id-ID")}`);

for (const file of args) {
  const mime = MIME[extname(file).toLowerCase()];
  if (!mime) {
    console.error(`skip ${file}: unsupported type`);
    continue;
  }
  const dataUrl = `data:${mime};base64,${(await readFile(file)).toString("base64")}`;
  try {
    const result = await extractReceipt(apiKey, dataUrl, { reasoningEffort: effort });
    const r = result.receipt;
    const sum = r.lines.reduce((acc, l) => acc + (l.line_total ?? 0), 0);
    const low = r.lines.filter((l) => l.confidence !== "high").length;
    console.log(`\n${basename(file)}  (${result.model}, effort ${effort}, ${(result.latencyMs / 1000).toFixed(1)} s)`);
    console.log(`  store ${r.store_name ?? "?"} ${r.store_branch ?? ""} | date ${r.purchase_date ?? "?"}`);
    console.log(`  lines ${r.lines.length} (${low} not high confidence) | sum ${rupiah(sum)} | printed total ${rupiah(r.total)} | ${sum === r.total ? "MATCH" : "MISMATCH"}`);
    console.log(`  tokens in ${result.usage.prompt_tokens} (cached ${result.usage.cached_tokens}) out ${result.usage.completion_tokens} (reasoning ${result.usage.reasoning_tokens})`);
    if (r.unreadable.length) console.log(`  unreadable: ${r.unreadable.join("; ")}`);
    await writeFile(join(outDir, `${basename(file, extname(file))}.json`), JSON.stringify(result, null, 2));
  } catch (err) {
    console.error(`\n${basename(file)}  FAILED: ${(err as Error).message}`);
  }
}
