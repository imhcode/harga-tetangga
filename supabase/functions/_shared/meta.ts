// Minimal Meta Model API client shared by Edge Functions (Deno) and tools/ scripts (Node).
// Uses plain fetch only, so it runs in both runtimes. Keep this file free of runtime-specific APIs.

export const META_BASE_URL = "https://api.meta.ai/v1";

const RETRYABLE = new Set([429, 500, 503]);

export class MetaApiError extends Error {
  status: number;
  body: string;
  constructor(status: number, body: string) {
    super(`Meta API ${status}: ${body.slice(0, 500)}`);
    this.status = status;
    this.body = body;
  }
}

export interface ChatCompletion {
  id: string;
  model: string;
  choices: {
    index: number;
    finish_reason: string;
    message: {
      role: string;
      content: string | null;
      tool_calls?: { id: string; type: string; function: { name: string; arguments: string } }[];
    };
  }[];
  usage?: {
    prompt_tokens: number;
    completion_tokens: number;
    total_tokens: number;
    prompt_tokens_details?: { cached_tokens?: number };
    completion_tokens_details?: { reasoning_tokens?: number };
  };
}

// Retries 429/500/503 with exponential backoff and honours Retry-After.
// Never retries 400/401/402/403/404/413 or 504 (see META-API.md section 3).
export async function metaFetch(
  path: string,
  apiKey: string,
  init: RequestInit = {},
  maxRetries = 3,
): Promise<Response> {
  for (let attempt = 0; ; attempt++) {
    const res = await fetch(`${META_BASE_URL}${path}`, {
      ...init,
      headers: {
        Authorization: `Bearer ${apiKey}`,
        "Content-Type": "application/json",
        ...(init.headers ?? {}),
      },
    });
    if (res.ok) return res;
    if (!RETRYABLE.has(res.status) || attempt >= maxRetries) {
      throw new MetaApiError(res.status, await res.text());
    }
    const retryAfter = Number(res.headers.get("retry-after"));
    const delay = retryAfter > 0 ? retryAfter * 1000 : 2 ** attempt * 500 + Math.random() * 250;
    await res.body?.cancel();
    await new Promise((resolve) => setTimeout(resolve, delay));
  }
}

export async function chatCompletion(
  apiKey: string,
  body: Record<string, unknown>,
): Promise<ChatCompletion> {
  const res = await metaFetch("/chat/completions", apiKey, {
    method: "POST",
    body: JSON.stringify(body),
  });
  return await res.json();
}

export async function listModels(apiKey: string): Promise<string[]> {
  const res = await metaFetch("/models", apiKey);
  const json = await res.json();
  return (json.data ?? []).map((m: { id: string }) => m.id);
}
