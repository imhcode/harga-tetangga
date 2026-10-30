# Receipt probe, 30 Oct 2026 (F0-03)

Model `muse-spark-1.3`, Chat Completions, inline JPEG, strict JSON schema from `supabase/functions/_shared/receipt.ts`. Line accuracy was checked by eye against the photos. Photos stay in the gitignored `receipts/` folder.

| Receipt | Type | Lines (items + discounts) | Correct at `low` | Total matches | `low` latency | `minimal` latency |
|---|---|---|---|---|---|---|
| A | Minimarket, crumpled paper | 10 (9 + 1) | 10/10 | yes | 11.8 s | 5.9 s |
| B | Indomaret, photographed on fabric | 9 (6 + 3) | 9/9 | yes | 9.5 s | 5.3 s |
| C | Café bill, multi-line item names | 5 (5 + 0) | 5/5 | yes | 8.4 s | 4.8 s |
| **All** | | **24** | **24/24 (100%)** | **3/3** | 9.9 s avg | 5.3 s avg |

## Findings

- Every line, quantity, price and discount was read correctly at `low`. Discount lines (`VC ... (2,000)`) came out as negative `discount` lines, so the line sum equals the printed total on all three.
- `minimal` gave the same values. Differences were cosmetic only: a trailing colon dropped from voucher names, and a unit price filled in where only a line total was printed (correct for quantity 1).
- `minimal` is about 2× faster and uses about half the output tokens (756–1,083 vs 1,425–2,361).
- Phone numbers and the masked card number were not copied into the output.
- `prompt_tokens_details.cached_tokens` reached 497 on repeat calls, so the stable instructions are being cached.

## Limits of this sample

- Three receipts, none from Kudus and none from the 4–5 target stores.
- One is a café bill, not a grocery receipt.
- All three photos are sharp. Faded thermal paper and long receipts are untested.
- The ≥85% target in the PRD needs the 10-receipt ground truth set (F1-10) before it counts.
