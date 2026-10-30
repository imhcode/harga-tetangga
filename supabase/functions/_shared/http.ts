// Helpers shared by Edge Functions: CORS, JSON responses and the server-only API key.

export const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

export function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

// The Meta key lives only in Edge Function secrets, never in the Android app.
export function modelApiKey(): string {
  const key = Deno.env.get("MODEL_API_KEY");
  if (!key) throw new Error("MODEL_API_KEY secret is not set");
  return key;
}
