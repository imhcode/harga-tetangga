# HargaTetangga

An Android app for households that buy staple groceries. It reads shopping receipts from a photo, remembers what each item cost at each store, and answers two questions before a trip: **how much cash to bring**, and **which store is cheapest for this list**. After the trip it compares the plan with the receipt.

Built solo for the Meta Global AI Developer Hackathon 2026 (Indie track, theme "Build for Your World") on the Meta Model API. Planning documents are in Indonesian: [PRD.md](PRD.md), [BACKLOG.md](BACKLOG.md), [META-API.md](META-API.md).

## Why

In our house in Kudus, Indonesia, the same item costs different amounts at different shops, and nobody remembers the last price. So we guess how much cash to bring. Bring too little and the trip stalls; bring too much and it goes on things that were never on the list. HargaTetangga turns receipts we already get into a price memory, and turns that memory into a cash estimate and a store plan.

## What it does

| Tab | What happens |
|---|---|
| **Belanja** (shopping) | Type or speak a list; the model splits it into items you tick. The estimate card shows cash to bring (upper price range + buffer, rounded up to Rp 5,000), the cheapest one- or two-store plan, and items with no price yet. Ask the assistant in plain Indonesian. "Berangkat belanja" saves the estimate before you leave. |
| **Struk** (receipt) | Photo or gallery image, black out personal details, send. The Review screen opens only the lines that need a look. After saving, the trip result compares the receipt with the list: cash enough or not, estimate vs paid, items outside the plan. |
| **Harga** (prices) | Latest confirmed price of every product at every store, with its age. |
| **Laporan** (report) | Monthly spend by category and product; star products as important and filter to them. |
| **Akun** (account) | Household (invite code, owner approves members), a log of every photo sent to the model, privacy notes. |

## How it uses Meta's models

Every model call goes through a Supabase Edge Function. The API key never ships in the app. All traffic uses Standard models over Chat Completions with inline images, so nothing is stored through the Files or Responses APIs.

| Step | Model and feature | What the model does | What code does around it |
|---|---|---|---|
| Read a receipt | `muse-spark-1.3`, vision + strict JSON schema, `reasoning_effort: minimal` | Lines, quantities, prices, discounts, store, date, with the printed text of each line | Checks line sum vs printed total, quantity × price vs line total, price ranges |
| Escalation cascade | Spark `low`, then `muse-wonder-1.0` | Re-reads only when the numbers above do not add up | Keeps the read with the fewest inconsistencies; logs every tier tried |
| Match to products | Spark + structured output | Maps "KNZLER SNGLES KJU 65" to the household catalog or proposes "Kanzler Singles Keju 65 g" | Confirmed aliases skip the model; only catalog references that exist are accepted |
| Parse a list | Spark + structured output | "telur sekilo, popmie ayam 3" → items with quantities and catalog links | Person ticks each item before it is added |
| Spoken list | `muse-voice-transcribe-1.0`, file endpoint, push-to-talk, Indonesian bias, the household's product words as keywords | Speech to text | Same parser and tick list as typed lists |
| Shopping assistant | Spark tool calling (`tool_choice: auto`) | Chooses between price lookup, cash estimate, store comparison and purchase history; proposes ingredients for a dish | Every number comes from a deterministic tool; arguments are validated; proposals become list items only through a button |

## Human in the loop

The model reads and suggests; a person decides every write.

| | Point | Rule |
|---|---|---|
| H1 | Sending a photo | Only after the preview and a tap on "Kirim ke AI"; covered areas are painted black first |
| H2 | Receipt result | Nothing is saved before the Review screen is confirmed |
| H3 | Aliases | Created only from confirmed or corrected lines |
| H4 | List items | Typed, spoken or proposed items are ticked before they join the list |
| H5 | Assistant | Reads and calculates only; changes to the list are proposals with a button |
| H6 | Cascade | A Wonder read still goes through Review |
| H7 | Household | A new member sees shared data only after the owner approves |

## Architecture

```
Android app (Kotlin, Jetpack Compose)
   │  Supabase Auth session
   ▼
Supabase Edge Functions (Deno)          MODEL_API_KEY lives here only
   extract-receipt   parse-list   voice-list   estimate   agent   trip-result
   │                                   │
   ├─► Meta Model API                  └─► deterministic estimate engine (_shared/estimate.ts)
   └─► Postgres with row level security
         stores, products, aliases, receipts, price_observations,
         shopping lists, estimates, trips, households
```

- Row level security limits every row to the household; raw receipts stay private to the person who sent them.
- `confirm_receipt` saves a reviewed receipt in one transaction and reuses stores and products other members already saved.

## Measured so far

| What | Result | Sample |
|---|---|---|
| Receipt lines read correctly (numbers) | 24 / 24 at every tier | 3 receipts, checked by hand against the photos ([metrics/2026-10-30-receipt-probe.md](metrics/2026-10-30-receipt-probe.md)) |
| Cascade cost and latency | Spark minimal ~$0.006 and 5.6 s per receipt; Wonder ~$0.036 and 8.1 s | Same 3 receipts ([metrics/2026-10-30-cascade.md](metrics/2026-10-30-cascade.md)) |
| Second read of a known receipt | 7 of 9 lines auto-accepted through aliases; 13.7 s → 7.0 s | Emulator test |

The sample is small and none of it comes from the target stores yet. Field-test results (cash enough, estimate error, share of items outside the plan) are computed by [tools/metrics.sql](tools/metrics.sql) and will be added here after the household trips.

## Privacy

- Photos are resized on the phone, covered areas are painted black, and the photo is deleted from the phone after saving.
- Standard-mode models only; Meta states Standard content is not used to train its models. Retention follows Meta's policy; this app makes no retention promise of its own.
- 18+ only (Meta Model API terms). The app records and compares prices; it does not give financial advice.
- No real receipts, keys or personal data are in this repository.

## Built during the hackathon

Everything in this repository was written during the event. The planning documents (IDEA, PLAN, PRD, BACKLOG, META-API notes) were written before kickoff without code and are committed as the first commit. All code commits are dated from 30 October 2026, after kickoff:

- Android app: capture, redaction, Review & Verify, prices, shopping list, voice input, assistant, trip result, report, household, onboarding
- Edge Functions: receipt extraction cascade, consistency checks, product matching, list parsing, voice, estimate engine, tool-calling agent, trip comparison
- Database schema, row level security and household functions (`supabase/migrations/`)
- Measurement tools: `tools/receipt-probe.ts`, `tools/accuracy.ts`, `tools/metrics.sql`

## Repository layout

| Path | What |
|---|---|
| `android/` | Android app |
| `supabase/functions/` | Edge Functions; `_shared/` holds the Meta client, prompts, schemas, estimate engine and agent |
| `supabase/migrations/` | Database schema and row level security |
| `tools/` | Node scripts for probing the API and measuring accuracy |
| `metrics/` | Recorded measurements |

## Setup

Requirements: JDK 17+, Android SDK (platform 35), Node 22+, a Supabase project, and a Meta Model API key with billing configured.

1. Check API access:
   ```bash
   cp .env.example .env   # fill in MODEL_API_KEY
   node --env-file=.env tools/check-models.ts
   ```
2. Supabase:
   ```bash
   npx supabase login
   npx supabase link --project-ref <project-ref>
   npx supabase db push --linked
   npx supabase secrets set MODEL_API_KEY=<key>
   for f in extract-receipt parse-list voice-list estimate agent trip-result; do npx supabase functions deploy $f --use-api; done
   ```
   Create a user in the dashboard (Authentication → Users → Add user, "Auto Confirm User").
3. Android: copy `android/local.properties.example` to `android/local.properties`, fill in `sdk.dir`, `SUPABASE_URL` and `SUPABASE_ANON_KEY`, then:
   ```bash
   cd android && ./gradlew installDebug
   ```
   `./gradlew assembleRelease` builds a release APK; it uses `android/keystore.properties` when present and the debug key otherwise.
4. Optional: probe receipt photos placed in `receipts/` (gitignored):
   ```bash
   node --env-file=.env tools/receipt-probe.ts receipts/*.jpg
   ```

## Known limits

- No sign-up screen yet; accounts are created in the Supabase dashboard.
- The receipt and estimate numbers above come from a handful of receipts; field data is still being collected.
- Price comparison only knows stores the household has receipts from; there is no map or distance, only a flat cost per extra store.
- Spoken lists were tested with synthesized speech; real Indonesian speech is being tested on phones.
