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

// The gateway's JWT check also accepts the public anon key, which ships inside the app.
// Model calls cost money, so require a real signed-in user. Returns the user id, or null.
export async function requireUser(req: Request): Promise<string | null> {
  const auth = req.headers.get("Authorization");
  if (!auth?.startsWith("Bearer ")) return null;
  const res = await fetch(`${Deno.env.get("SUPABASE_URL")}/auth/v1/user`, {
    headers: { Authorization: auth, apikey: Deno.env.get("SUPABASE_ANON_KEY") ?? "" },
  });
  if (!res.ok) {
    await res.body?.cancel();
    return null;
  }
  const user = await res.json();
  return typeof user?.id === "string" ? user.id : null;
}

// The Meta key lives only in Edge Function secrets, never in the Android app.
export function modelApiKey(): string {
  const key = Deno.env.get("MODEL_API_KEY");
  if (!key) throw new Error("MODEL_API_KEY secret is not set");
  return key;
}
