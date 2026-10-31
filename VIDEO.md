# Demo video script (3:00)

Rules that shape this video (from the Participant Guide, see IDEA.md): at most 3 minutes, shows only what was built during the event, no secrets or real personal data, and everyone shown has agreed. Narration is in English for the judges; the app is in Indonesian, so add short English captions for on-screen text.

Placeholders in [brackets] are filled from the field test (`npx supabase db query --linked -f tools/metrics.sql`).

## Before recording

- [ ] Written consent (a chat message is enough) from everyone who appears or whose receipts appear: what is shown, that it is public, and that the organizers may reuse it.
- [ ] Receipts on camera: own receipts only, card and member numbers covered with the in-app black box and, if needed, blurred in editing.
- [ ] Two real receipts from two different Kudus stores already saved, so the Prices tab shows a "Termurah" label.
- [ ] Phone screen recording: `adb shell screenrecord /sdcard/demo.mp4` (stops at 3 minutes per file) or the phone's own recorder.
- [ ] Record a rough cut first (D9 morning), then the final; keep the rough cut as backup.

## Shot list

| Time | Shot | Narration (English) | On screen |
|---|---|---|---|
| 0:00–0:20 | My wife at the front door with her wallet, a pile of receipts on the table | "In Kudus, the same rice costs different amounts at every shop, and nobody remembers last week's price. So before every trip we guess how much cash to bring. Too little and the trip stalls. Too much, and it goes on things that were never on the list." | Title: HargaTetangga |
| 0:20–0:50 | Phone: Struk tab → photo of a real receipt → drag a black box over the card number → Kirim ke AI | "Every receipt becomes price memory. She covers the card number herself; that part never leaves the phone. Muse Spark reads the photo into a strict JSON schema, and plain code checks the maths: every line adds up to the printed total." | Caption: "Black box = never sent" |
| 0:50–1:10 | Review screen: flagged lines open, auto-accepted lines collapsed → Simpan | "The model never saves anything by itself. Only lines that need a look are opened. If the numbers don't add up, the app re-reads with more reasoning, and only then escalates to Muse Wonder." | Caption: "AI reads, a person confirms" |
| 1:10–1:25 | Prices tab: one product at two stores, "Termurah" tag, age of each price | "A confirmed name becomes an alias, so the next receipt from that shop is recognised without asking the model." | |
| 1:25–1:55 | Belanja tab: tap Bicara, she says "beras lima kilo, minyak goreng dua liter, telur sekilo" → tick list → estimate card | "Next week's list, spoken in Indonesian. Muse Voice Transcribe turns it into text, Spark turns it into items, and she ticks what she wants. The answer: bring Rp [X], shop at [store], and these two items have no price yet, so they are not guessed." | Caption: "Bring Rp [X]" |
| 1:55–2:20 | Assistant: "Mau masak sayur sop, beli apa saja?" → proposals → untick what's at home → add. Then "Belanja di mana paling murah?" | "The assistant uses tool calling. It can look up prices, run the estimate and compare stores, but every number comes from those tools, not from the model's imagination. It can only propose items; she decides." | Caption: "Numbers from tools, decisions by people" |
| 2:20–2:45 | After the real trip: receipt → Bandingkan dengan rencana → trip result | "After the trip the receipt is compared with the plan. This time she brought Rp [X] and paid Rp [Y]. The estimate was [Z]% off, and [N] items were outside the plan. A note, not a judgement." | Real numbers from her trip |
| 2:45–3:00 | Akun tab: household code, my phone approving her request; Laporan tab | "One household, shared prices, private receipts. Over [N] trips: cash was enough [N] times, and the estimate was off by a median [Z]%. HargaTetangga: know what to bring before you go." | Repo link |

## What to say if something fails on camera

Keep the take. Show the Review screen catching it: that is the point of the human-in-the-loop design.
