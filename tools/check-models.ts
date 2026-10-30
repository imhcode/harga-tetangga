// F0-02: confirms the Meta Model API is reachable with this key from this location.
// Usage: node --env-file=.env tools/check-models.ts

import { listModels } from "../supabase/functions/_shared/meta.ts";

const apiKey = process.env.MODEL_API_KEY;
if (!apiKey) {
  console.error("MODEL_API_KEY is not set. Copy .env.example to .env and fill it in.");
  process.exit(1);
}

const models = await listModels(apiKey);
console.log(`${models.length} models available:`);
for (const id of models.sort()) console.log(`  ${id}`);

for (const needed of ["muse-spark-1.3", "muse-wonder-1.0", "muse-voice-transcribe-1.0"]) {
  console.log(`${models.includes(needed) ? "OK     " : "MISSING"} ${needed}`);
}
