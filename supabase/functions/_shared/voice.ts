// F4-03: spoken shopping list -> text, with Muse Voice Transcribe (file endpoint, push-to-talk).
// Plain fetch with FormData so it runs in Deno and Node alike.

import { META_BASE_URL, MetaApiError } from "./meta.ts";

export const VOICE_MODEL = "muse-voice-transcribe-1.0";

export async function transcribeWav(
  apiKey: string,
  wav: Uint8Array,
  keywords: string[] = [],
): Promise<{ transcript: string; ms: number }> {
  const started = Date.now();
  const form = new FormData();
  form.append(
    "request",
    new Blob([JSON.stringify({
      model: VOICE_MODEL,
      audioEncoding: "WAV",
      mode: "PUSH_TO_TALK",
      languageBias: ["Indonesian"],
      // Product words the person already buys; biasing does not guarantee spelling.
      keywords: keywords.slice(0, 100),
    })], { type: "application/json" }),
  );
  form.append("audio", new Blob([wav], { type: "audio/wav" }), "daftar.wav");

  for (let attempt = 0; ; attempt++) {
    const res = await fetch(`${META_BASE_URL}/asr/transcribe`, {
      method: "POST",
      headers: { Authorization: `Bearer ${apiKey}`, Accept: "application/json" },
      body: form,
    });
    if (res.ok) {
      const json = await res.json();
      return { transcript: String(json.transcript ?? "").trim(), ms: Date.now() - started };
    }
    // Only the concurrency/hourly limit is worth one retry here.
    if (res.status !== 429 || attempt >= 1) throw new MetaApiError(res.status, await res.text());
    await res.body?.cancel();
    await new Promise((resolve) => setTimeout(resolve, 1500));
  }
}
