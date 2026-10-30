// F0-05: smallest end-to-end call. App -> this function -> Muse Spark -> app.
// JWT verification stays on (Supabase default), so only signed-in users can call it.

import { chatCompletion, MetaApiError } from "../_shared/meta.ts";
import { corsHeaders, json, modelApiKey } from "../_shared/http.ts";

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });

  const { prompt } = await req.json().catch(() => ({ prompt: undefined }));
  const started = Date.now();
  try {
    const completion = await chatCompletion(modelApiKey(), {
      model: "muse-spark-1.3",
      reasoning_effort: "minimal",
      max_completion_tokens: 2000,
      messages: [
        { role: "developer", content: "Jawab dalam satu kalimat pendek berbahasa Indonesia." },
        { role: "user", content: typeof prompt === "string" && prompt ? prompt : "Sebutkan satu kebutuhan pokok dapur." },
      ],
    });
    return json({
      reply: completion.choices[0]?.message.content ?? "",
      model: completion.model,
      latency_ms: Date.now() - started,
      usage: completion.usage,
    });
  } catch (err) {
    const status = err instanceof MetaApiError ? err.status : 500;
    return json({ error: (err as Error).message }, status === 401 ? 502 : status);
  }
});
