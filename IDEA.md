# HargaTetangga ("Neighbor Prices")

> Know where to shop, from people you trust.

Meta Global AI Developer Hackathon 2026 — idea draft.

- **Track:** Indie Developer (individuals, students, or teams of up to 4; one Representative submits). Building solo.
- **Problem statement / theme:** **Build for Your World** (bring Meta's models into your own domain; solve a problem you know firsthand)
- **Status:** accepted participant (application deadline was Oct 25, 2026)
- **Hackathon:** kickoff Oct 26, building period Oct 27 – Nov 4, 2026
- **Submission deadline:** Nov 4, 2026, **9:00 PM ET** (= Nov 5, 09:00 WIB), via the Cerebral Valley submit portal. Late submissions are not considered; do not rely on any grace period.

---

## Official rules that shape this project

Sources: [official waiver](https://cerebralvalley.ai/e/meta-ai-developer-hackathon/waiver) and the event's Participant Guide (`/details`). Not legal advice; read the originals before agreeing.

**Rules that can disqualify us (Participant Guide)**

- **New work only.** What we present must be built during the event. The demo may only highlight features, code and functionality built during the Hackathon, and judges must be able to tell clearly what is new. Failing to identify our original contributions means **immediate disqualification**. Only the Startup track may build on an existing product; we are Indie. So: a **fresh repo with commits starting at kickoff**, no code pasted from earlier projects (planning documents such as this file are fine).
- **Public repository.** The repo must be public, with no secrets, no `.env`, and no real receipts.
- **Rights to data and assets.** Projects are disqualified if they use code, data or assets we do not have the rights to. This affects where the starting price data comes from (see "Data sources and rights" below).
- **Demo video: 3 minutes**, highlighting what we built during the event. It is one of the primary artifacts judges use, alongside the code and the live product.
- **Everyone who worked on the project must be listed** on the submission page; verify that the repo and the video link open without login.

**How projects are judged (Participant Guide)**

1. **Stage 1, pass/fail:** the project reasonably fits the theme and reasonably uses the Meta Model API and/or Meta open-weight models, built during the Hackathon.
2. **Stage 2, scored 0–10** against the rubric below.
3. **Stage 3:** the top 5 per track are judged again live from their demo videos, using the same rubric.

**Sample "anti-projects" (generic or policy-risky; avoid building these)**

AI medical advice; **basic RAG apps**; **simple image analyzers**; simple "AI for education" chatbots; AI job application screeners; AI nutrition coaches; personality analyzers; sports analyzers or coaches; anything political or involving war.

> **Risk for us: "simple image analyzer."** A receipt scanner that only reads a photo and shows the text could be read as exactly that. HargaTetangga must be judged on the **decision layer**, not the scan: it plans where to shop for a whole list, with sources, freshness and a verifiable cost breakdown. See "Differentiation from the anti-projects" under "What makes it different."

**Judging criteria (projects)**

| Criterion | Weight | What judges look for | How HargaTetangga answers it |
|---|---|---|---|
| Impact | 30% | Real-world potential, who benefits, could people use it, **fits one of the problem statements** | Household grocery savings; field test with real households. Fits **Build for Your World**: a problem we know firsthand |
| Use of Meta AI Tooling | 25% | Creative use, **beyond a basic integration**, unexpected capabilities | Spark + SAM + Voice Transcribe together, not just OCR (see "How we use Meta's models") |
| Demo | 25% | Working, impressive demo that holds up live; demo video quality | Live receipt → recommendation flow; real testers in the video |
| Technical Depth & Execution | 20% | Sound, refined engineering | Normalization pipeline, unit conversion, explainable store optimizer |

**Prizes (Indie track, $400k total):** 1st $150k · 2nd $100k · 3rd $75k · 4th $50k · 5th $25k. Non-US winners provide a W-8BEN and pay their own taxes and transfer fees.

**Bonus prizes ($50k, on top of the track prizes)**

| Bonus | Prize | Worth chasing? |
|---|---|---|
| Most Viral Project | $20k | Yes, it is cheap: one X post with the demo, link submitted on the platform; engagement counted until Nov 8 end of day |
| Community Vote | $10k | Maybe: one vote per verified account in the public gallery; winner announced Nov 16 |
| Best Agentic Use-Case | $5k | Yes, if the shopping planner is a tool-calling agent (see "Technical approach") |
| Best Build on Meta Open Weight Models | $10k | No for now: needs Muse Glimmer / Muse Spark Open on own hardware or partner inference |
| Best Creative Build | $5k | No: only for Muse Image / Muse Wonder projects |

**Eligibility:** 18+ (and of legal age locally); not in a sanctioned country/region; no Meta or Cerebral Valley staff or their families. Credits ($150) are valid for a 10-day development window; read the separate Credit Terms before claiming.

**Licensing and IP (what we are agreeing to)**

- We keep ownership of the submission, but grant the organizers a **perpetual, irrevocable, worldwide, royalty-free license** to use it for any purpose, including marketing.
- Everything shared during the event is **non-confidential**. Do not put API keys, secrets or private business data in the repo, demo or video.
- We must warrant the submission is original and **does not violate anyone's privacy or IP rights**. Owning our earlier code satisfies the IP warranty, but the event's **new-work-only rule still applies**, so earlier code does not go into this submission. Open source is fine if its license is followed and we build on top of it.
- Name, likeness, voice and recordings can be used by organizers in perpetuity; contact details may be shared with sponsors for marketing.

---

## Short version (~100 words, for the application form)

**HargaTetangga ("Neighbor Prices") is a shopping decision assistant for households, powered by trusted circles.**

Families in Indonesia shop across minimarkets, supermarkets, traditional markets and small warungs, and prices for the same item vary widely between stores just a few kilometers apart. HargaTetangga turns a photo of a receipt into structured price data with Muse Spark. It first helps each user track their own spending and price history. When users connect with friends and neighbors, they share item prices (never full receipts) inside trusted circles. Given a shopping list, the app recommends the cheapest store, or combination of stores, including travel cost.

---

## One-liner

A shopping decision assistant that learns real prices from receipts you and people you trust have scanned, then tells you which store (or combination of stores) is cheapest for your shopping list, including the cost of getting there.

## The problem

In Indonesia, everyday shopping is spread across minimarket chains, supermarkets, traditional markets (*pasar*) and neighborhood warungs. The same item (cooking oil, rice, eggs, baby formula) can cost noticeably different amounts at stores a few minutes apart, and promos change weekly. [Optional: food makes up roughly half of household spending in Indonesia, based on BPS data; verify the exact figure.]

Today, mothers who manage the household budget rely on memory and on WhatsApp groups ("Where's cooking oil cheap this week?"). That information is useful but scattered, unstructured, and lost within days. Existing tools don't solve this either:

- Government price panels report city- or province-level averages for staples only.
- Retailer apps only show their own prices.
- Expense trackers require manual input and stop at totals.

## The solution

HargaTetangga is built in **three layers**, so it is useful from day one and becomes more valuable as people connect:

1. **Personal:** Snap a receipt, and the app automatically logs spending, item-level price history and where you last bought each item cheapest. It also tracks a monthly budget. One user alone already gets value.
2. **Circles:** When you meet other mothers (school, *arisan*, neighborhood), invite them via a WhatsApp link or QR code. Circle members share **item prices only, never full receipts or totals**. Prices from people you know are trustworthy.
3. **Neighborhood (opt-in):** Anonymous, aggregated prices per district. A price is shown only after **at least 3 different contributors** report it. This protects individual shopping habits and filters out bad data.

**The core feature:** Type or say a shopping list ("5kg rice, 2L cooking oil, 1kg eggs, milk"). The app compares every nearby option: a single store, or 2–3 store combinations. It weighs total price against travel distance and explains the trade-off:

> *"Cheapest single trip: Store B, Rp 187k. Store A + B saves Rp 8k more but adds 2.4 km, probably not worth it."*

Every price shows its source and freshness ("from Rina's circle, 2 days ago").

## How we use Meta's models

- **Muse Spark (vision + structured output):** reads messy receipt photos (and price photos we are allowed to use; see "Data sources and rights"). It extracts store, date, items and prices into JSON, then normalizes cryptic receipt names (e.g. `INDMIE GRG SPL 85G` → *Indomie Goreng Spesial, 85g*) to a canonical product with brand, variant and size.
- **Muse Spark (reasoning):** parses free-form shopping lists into canonical products. It also explains recommendations in plain, friendly language.
- **Muse Voice Transcribe:** voice-note input for shopping lists, and for prices from traditional markets that don't issue receipts ("chili 60k per kilo at Pasar X").
- **SAM 3.1 (segmentation):** isolates individual price tags on shelves and handwritten price boards in traditional markets, so each tag can be read on its own. This covers retail that never prints a receipt, and goes beyond plain OCR. The Participant Guide lists SAM 3.1 among the Meta Model API models; TODO: prototype early and confirm access and pricing in the API docs. It is a stretch goal, not a dependency of the main flow.

**Judging angle ("beyond a basic integration"):** the models work as one pipeline. SAM finds price tags, Spark reads and normalizes them into canonical products, Voice Transcribe captures spoken prices and lists, and Spark reasons over the results to explain the recommendation.

## What makes it different

- **Decision, not just lookup:** it doesn't only show prices. It optimizes *where to shop* for your whole list, including travel cost.
- **Trust-based data:** prices come from people you actually know, not anonymous strangers.
- **Personal-first growth:** useful to one person on day one, so it avoids the cold-start problem that killed earlier crowdsourced price apps.
- **Covers informal retail:** traditional markets and warungs are included through photos and voice, not just chains with online catalogs.
- **Privacy by design:** you control what is shared, at the item level; aggregated data requires k ≥ 3 contributors.

### Differentiation from the anti-projects

The Participant Guide lists "simple image analyzers" and "basic RAG apps" as generic. Our answer is that the scan is only the input; the value is the **decision**:

- A shopping list goes to a **planner agent** (Muse Spark with function calling) that calls tools to look up prices, compute the extra cost of an additional store, and propose substitutes, then returns a plan whose numbers can be checked against the stored prices. This is the "verifiable agent" idea, and it targets the Best Agentic Use-Case bonus.
- Prices carry **source, age and reporter count**; missing items are flagged, never guessed.
- Receipt normalization uses an **alias table that the user verifies**, so the model's guesses become reviewed data (there is no embedding model in the Meta Model API, so we do not rely on vector search).
- No retrieval-over-documents chat: nothing in the product is a "chat with your receipts" feature.

TODO: decide how much of the agent loop fits the 10-day, solo, ~4 h/day budget; keep the scan-and-verify flow as the fallback demo.

### Data sources and rights

The guide disqualifies projects that use data or assets we have no rights to, and the waiver makes us warrant that we do not violate anyone's IP or privacy.

- **Do not** scrape or photograph retailers' promo catalogs, flyers or app screens to seed the database, and do not copy price lists from retailer apps or websites.
- **Use** our own receipts, receipts from testers who consented, and photos of shelf labels or price boards that we take ourselves in stores for personal, factual price notes. For anything public (repo, demo, video), use anonymized or synthetic receipts.
- Prices are facts that users report; we store item, store, price and date, not retailers' creative content (catalog images, product photos, descriptions).
- If a store's terms or owner object to photographing labels, stop and use receipts only.

### Comparison with existing options

| Existing option | What it lacks | HargaTetangga |
|---|---|---|
| Receipt-scan apps (e.g. Shooper) | Data serves brand analytics; users get rewards, not shopping decisions | Store / combination recommendation including distance, for your list |
| Government price panels | City/province averages, staples only | Per-store, per-district prices, including branded goods |
| Retailer apps (Alfagift, Klik Indomaret) | Only their own prices | Cross-store comparison, including markets and warungs |
| Expense trackers | Manual input, totals only | Automatic from a photo, down to item-level prices |
| Mothers' WhatsApp groups | Info gets buried in chat, unstructured | Same trusted info, but stored, structured and computed |

## Technical approach

- **Frontend:** Next.js Progressive Web App (camera access, installable, no app store needed)
- **Backend:** Supabase (Postgres, auth, storage), with Row Level Security enforcing private / circle / public visibility for each price observation
- **AI pipeline:** receipt image → Muse Spark structured extraction → product normalization and unit conversion (price per kg/L) → stored as price observations
- **Recommendation engine:**
  - Enumerates all 1–3 store combinations among nearby stores (typically fewer than 10 stores, so exact search is fast and explainable).
  - Cost = sum of the cheapest available item prices + travel cost (distance × per-km cost).
  - Missing items are flagged, never guessed.
- **Planner agent (proposal):** Muse Spark with function calling over deterministic tools (`lookup_prices`, `extra_store_cost`, `suggest_substitute`). The optimizer above stays plain code; the agent decides which tools to call, and its final answer must cite the prices it used so the numbers can be verified. Use Chat Completions with inline base64 images (see the project's API notes).
- **Freshness:** older price observations are down-weighted; every price displays its age and number of reporters.

### Core data model

- `users`, `circles`, `circle_members`
- `stores`: name, coordinates, type (minimarket / supermarket / pasar / warung)
- `products`: canonical product (brand, variant, size, unit)
- `receipts`: photo and raw extraction (visible to owner only)
- `price_observations`: product, store, price, unit price, date, reporter, **visibility** (`private` / `circle` / `public`)

## Demo scope (by November 4)

- Receipt scan → structured, normalized price data, tested on [30–50] real receipts from [5] stores in one district (with tester consent)
- Shelf-tag / price-board photo → SAM segmentation → price per item (at least one traditional market or warung example)
- Personal dashboard: spending log and item price history
- Circles: invite via WhatsApp link, shared item prices
- Shopping list (text or voice) → cheapest store / combination recommendation with map and savings breakdown
- Field test with [3–5] real households, with measured savings (e.g. "[X]% cheaper on a typical weekly list")

## Impact

Saving even [5–10]% on weekly groceries matters to households on tight budgets. The model works anywhere informal and formal retail sit side by side, across Indonesia and much of Southeast Asia, South Asia and Latin America.

## Responsible design

- Personal data such as names, member numbers and card digits printed on receipts is stripped before storage.
- **Tester consent:** every household that contributes receipts or photos for the demo gives written consent (a short form or chat message) that says what is collected, how it is used, and that it may appear anonymized in the demo and video. This matters because the waiver makes us warrant that the submission does not violate anyone's privacy rights.
- **No real personal data in the public repo, demo or video.** Use anonymized or synthetic receipts for public material; the license we grant organizers is perpetual and the content is treated as non-confidential.
- Testers can ask for their data to be deleted at any time.
- Users choose exactly what to share, per circle.
- Neighborhood data is aggregated and shown only with at least 3 contributors.
- No selling of individual shopping data.

## About the builder

[1–2 sentences: your background, e.g. full-stack developer who has shipped [HR system / social media management tool for Threads / game project]. Add GitHub and LinkedIn links.]

---

## Build checklist

**Accounts & access**

- [ ] Join the hackathon Discord (#intros, #questions; office hours 10–11 AM ET on weekdays)
- [ ] **Claim the $150 Meta Model API credits** via the claim form, by Nov 4, 9:00 PM ET (each participant claims their own; credits arrive in batches; a payment method is required before the first call; set a spend limit on the Billing page)
- [ ] Test API access from Indonesia today (`GET /v1/models`); if it fails, fall back to open-weight models via Baseten / Modal / Fireworks (request their credits now; Baseten model id for the hackathon is `meta-models/muse-spark-open-1026`)
- [ ] Read the Credit Terms PDF
- [ ] Meta Model API key: Muse Spark, Muse Voice Transcribe, SAM
- [ ] Create a **fresh public repo** and make the first commit at kickoff, so the commit history proves new work only
- [ ] Maps / distance: OpenStreetMap or Mapbox free tier (straight-line distance × 1.3 is fine for the demo)
- [ ] Supabase project, Vercel (or similar) hosting

**Hardest parts — do these first**

1. [ ] Receipt line → canonical product matching (everything else depends on it)
2. [ ] Unit conversion (5 kg vs 2 × 2.5 kg vs per liter) so prices compare fairly

**Data & people for the demo**

- [ ] Collect [30–50] real receipts from [5] stores in one district (our own and consenting testers'); no retailer catalogs or flyers
- [ ] Recruit [3–5] real households as testers; get their written consent before collecting anything; record short testimonials for the video
- [ ] Measure savings on a typical weekly list

**Submission**

- [ ] Working demo (deployed PWA) that holds up live
- [ ] **3-minute demo video** that shows only what was built during the event: problem → scan receipt → shopping list → planner recommendation with cited prices → savings. Reserve at least 6 hours for recording and the README; record a rough cut early
- [ ] **Public** GitHub repo with setup instructions; no secrets, no real personal data, licenses of any open-source parts respected, and a README section listing what was built during the Hackathon
- [ ] Short write-up that states the problem statement (**Build for Your World**) and how we go beyond a basic Meta API integration (and why this is not a "simple image analyzer")
- [ ] Verify before submitting: repo opens logged out, video link opens, everyone who worked on it is listed
- [ ] Submit via the Cerebral Valley portal by Nov 4, 9:00 PM ET (Nov 5, 09:00 WIB); the last full day for submission questions is Nov 3
- [ ] Most Viral bonus: post the demo on X and submit the post link on the platform (engagement counts until Nov 8)
- [ ] If we win: W-8BEN and tax/transfer paperwork
