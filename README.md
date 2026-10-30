# HargaTetangga

An Android app for households that buy staple groceries. It reads shopping receipts from a photo, remembers what each item cost at each store, and answers two questions before a shopping trip: how much cash to bring, and which store is cheapest for the list.

Built during the Meta Global AI Developer Hackathon 2026 (Oct 27 – Nov 4) on the Meta Model API. Planning documents (Indonesian): [PRD.md](PRD.md), [BACKLOG.md](BACKLOG.md), [META-API.md](META-API.md).

## Architecture

```
Android app (Kotlin, Jetpack Compose)
   │  Supabase Auth session
   ▼
Supabase Edge Functions  ── MODEL_API_KEY (server secret only)
   │
   ├─► Meta Model API (muse-spark-1.3, Standard mode)
   └─► Postgres (prices, aliases, lists)
```

The Meta API key never ships in the app. Every model call goes through an Edge Function.

## Repository layout

| Path | What |
|---|---|
| `android/` | Android app |
| `supabase/functions/` | Edge Functions; `_shared/` holds the Meta API client and receipt schema |
| `tools/` | Node scripts for probing the API and measuring accuracy |

## Setup

Requirements: JDK 17+, Android SDK (platform 35), Node 22+, a Supabase project, a Meta Model API key with billing configured.

### 1. Check API access

```bash
cp .env.example .env   # then fill in MODEL_API_KEY
node --env-file=.env tools/check-models.ts
```

### 2. Probe receipt extraction (optional)

Put receipt photos in `receipts/` (gitignored; real receipts never enter the repo):

```bash
node --env-file=.env tools/receipt-probe.ts receipts/*.jpg
```

### 3. Supabase

```bash
npx supabase login
npx supabase link --project-ref <project-ref>
npx supabase secrets set MODEL_API_KEY=<key>
npx supabase functions deploy ping-model
```

Create the first user in the Supabase dashboard: Authentication → Users → Add user, with "Auto Confirm User" checked.

### 4. Android app

Copy `android/local.properties.example` to `android/local.properties` and fill in `sdk.dir`, `SUPABASE_URL` and `SUPABASE_ANON_KEY` (Project Settings → API). Then, with a phone connected over USB debugging:

```bash
cd android && ./gradlew installDebug
```

## Privacy

- All user traffic uses Meta's Standard models, which are not trained on customer content.
- Receipt photos are sent inline and are not stored through the Meta Files API.
- AI output is never saved without a person confirming it on the Review screen.
