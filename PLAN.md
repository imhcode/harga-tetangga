# HargaTetangga — Rencana Build (hasil brainstorming 22 Okt 2026)

Status: **sudah diterima** di Meta Global AI Developer Hackathon 2026. Build: 26 Okt – 4 Nov (submit 4 Nov, 9 PM ET).
Pembangun: 1 orang, ±4 jam/hari (≈40 jam total), dibantu AI. Pengguna pertama: istri (belanja pokok). Lokasi uji: Kudus.
Prioritas untuk juri: **(1) pemakaian model API yang bermakna, (2) dampak personal nyata.**

`IDEA.md` tetap sebagai draft asli. File ini adalah scope yang sudah dipersempit.

---

## Keputusan hasil brainstorming

| Topik | Keputusan | Alasan |
|---|---|---|
| Fokus belanja | **Belanja pokok** (mingguan/bulanan, barang kemasan) | Keranjang 10–15 barang, di situlah selisih Rp 1.000 × N jadi berarti. Sayur/lauk segar tidak masuk optimizer. |
| Cara input data | **Foto → ekstraksi AI → layar Review & Verify → simpan** | Akurasi AI tidak harus sempurna; koreksi manusia sekali menghasilkan alias permanen. |
| "Training" produk | **Tidak ada training.** Katalog kanonik + tabel alias per toko + model sebagai pemilih dari shortlist | Realistis 10 hari, dan lebih mudah diaudit. |
| Jarak | **Tanpa peta.** Biaya tetap per toko tambahan (default Rp 3.000, bisa diatur) | Hemat waktu; Kudus relatif dekat. |
| Label harga rak | **Stretch** (hari 8+), memakai pipeline Review & Verify yang sama | Menutup lubang data (harga barang yang belum pernah dibeli di toko itu). |
| "Masak apa?" | **Versi tipis**: menu → bahan → dipetakan ke produk di DB; yang tidak ada ditandai "belum ada data" | Hook demo kuat, tapi jangan melebar ke produk segar. |
| Promo & grup tetangga | **Visi saja**, bukan build. Demo berbagi cukup 2 akun (kamu & istri) | Butuh pengguna/sumber data tambahan. |
| Neighborhood (k ≥ 3) | Dijelaskan di visi, tidak dibangun | Tidak mungkin diuji dalam 10 hari. |

## Alur utama demo (2–3 menit)

1. **Foto struk** → Muse Spark mengekstrak toko, tanggal, item, harga → layar **Review & Verify**: hanya baris yang tidak yakin yang disorot; baris yakin otomatis diterima. Konfirmasi → tersimpan + alias belajar.
2. **Daftar belanja pokok** (ketik atau suara) → rekomendasi toko tunggal dan kombinasi 2 toko, dengan penjelasan ramah dan "potensi hemat Rp X". Item tanpa data ditandai, tidak ditebak. Setiap harga menampilkan sumber dan umur.
3. **"Masak apa?"** (tipis) → menu → bahan → masuk ke daftar belanja → rekomendasi toko.
4. **Berbagi** (stretch): akun kedua melihat harga yang dibagikan.

## Pemakaian model (untuk dibuktikan dengan angka)

- **Vision + structured output:** struk (dan label rak) → JSON.
- **Reasoning:** memilih produk kanonik dari shortlist, parsing daftar belanja, menu → bahan, menjelaskan rekomendasi.
- **Voice Transcribe:** daftar belanja lisan.
- **Metrik yang dicatat:** % baris struk yang otomatis terpetakan benar, % yang dikoreksi, waktu verifikasi per struk, tren akurasi seiring alias bertambah.
- **Embedding:** tidak tersedia di Meta API (lihat Temuan riset). Shortlist dengan pg_trgm lalu model memilih.

## Model data (ringkas)

- `stores`: nama, tipe (minimarket/supermarket/pasar/warung), kota
- `products`: kanonik (brand, varian, ukuran, satuan, kategori)
- `product_aliases`: teks mentah struk → produk, per toko, jumlah konfirmasi, siapa yang konfirmasi
- `receipts`: foto + ekstraksi mentah (hanya pemilik)
- `price_observations`: produk, toko, harga, harga per satuan, tanggal, sumber (struk/label rak/suara), status (verified/belum), visibilitas (private/circle)
- `circles`, `circle_members` (minimal)

## Mesin rekomendasi

- Enumerasi semua kombinasi 1–2 toko (opsional 3). Biaya = jumlah harga termurah per item + biaya tetap per toko tambahan.
- Item tanpa data → ditandai, tidak ditebak. Harga lama → ditandai umurnya.
- **Pengukuran jujur:** untuk setiap trip nyata, hitung "potensi hemat" (andai dibeli di toko terbaik menurut data) dan pisahkan dari penghematan yang benar-benar terjadi.

## Sebelum hackathon (22–26 Okt) — tanpa menulis kode

- [ ] **Baca aturan hackathon** soal kode/pekerjaan sebelum periode mulai. Jangan menulis kode sebelum jelas.
- [ ] **Eksperimen harga (1 sore):** 15 barang pokok × 4–5 toko di Kudus, dicatat di [template-harga-15x5.xlsx](template-harga-15x5.xlsx) (ringkasan dan vonis otomatis). Selisih besar antar format? Hasilnya mengonfirmasi atau menggeser wedge.
- [ ] Tentukan 4–5 toko uji (nama dan tipe): `TBD`
- [ ] Kumpulkan ±30 struk nyata (milik sendiri dan orang lain, termasuk toko berbeda) sebagai test set; mulai simpan semua struk mulai sekarang.
- [ ] Daftar seed ±50 produk pokok dan variasi ukuran (beras, minyak, telur, gula, tepung, mie instan, susu, kopi, teh, kecap, deterjen, sabun, dst.).
- [ ] Rekrut 2–3 rumah tangga penguji (mulai dari keluarga sendiri).

## Rencana 10 hari (±4 jam/hari)

| Hari | Fokus | Selesai jika |
|---|---|---|
| D1 (Sen 26 Okt, malam WIB) | Baca rules, setup, struk → JSON, uji test set | Rules dipahami; skema ekstraksi jalan |
| D2 (Sel 27) | Pemetaan produk kanonik + alias + konversi satuan | Angka akurasi otomatis tercatat |
| D3 (Rab 28) | DB, layar Review & Verify | 1 struk end-to-end tersimpan |
| D4 (Kam 29) | Daftar belanja (teks) → mesin rekomendasi | Hasil benar pada contoh manual |
| D5 (Jum 30) | Penjelasan, suara, sumber/umur harga; gerbang: alur inti jalan di HP | Siap uji lapangan |
| D6–D7 (Sab 31 Okt–Min 1 Nov) | Uji lapangan belanja bulanan (gajian) + perbaikan | Trip nyata tercatat; angka hemat dan akurasi |
| D8 (Sen 2 Nov) | "Masak apa?" tipis + berbagi 2 akun; label rak jika waktu; fitur dibekukan | Alur lengkap, atau fitur dipotong |
| D9 (Sel 3) | Polish, rekam video (+ cadangan), README | Video dan README jadi |
| D10 (Rab 4 Nov) | Tulisan singkat, kirim sebelum 22:00 WIB (tenggat ≈ Kam 5 Nov 09:00 WIB) | Terkirim dan dikonfirmasi |

Detail hitung mundur 22–26 Okt, gerbang go/pivot, ritme harian, rencana B, dan log metrik: lihat [PERSIAPAN.md](PERSIAPAN.md).

## Kriteria gagal / pivot

- Selisih harga antar toko pada eksperimen **< 3%** → geser wedge ke promo/pasar/belanja segar, atau ke "kapan beli".
- Akurasi pemetaan otomatis **< 60%** di akhir hari 2 → perkuat UI verify dan persempit katalog, jangan menambah fitur.
- Istri/penguji tidak mau memotret struk dalam seminggu → catat sebagai temuan jujur, dan uji alur dengan struk yang dikumpulkan.

## Temuan riset (22 Okt 2026)

> Riset lanjutan yang jauh lebih lengkap (±40 halaman dokumentasi, harga, batas, hukum/privasi, daftar uji hari 1): **[META-API.md](META-API.md)**. Ringkasan perubahan terhadap rencana: semua traffic pengguna lewat model **Standard** (Contributor melatih model Meta dan melarang data pribadi); ekstraksi lewat **Chat Completions** tanpa status dengan gambar inline (Responses menyimpan respons secara default, Files API menyimpan file); semua panggilan lewat server; metode pembayaran wajib dipasang sebelum request pertama; butuh kebijakan privasi dan batas 18+.

Sumber: halaman hackathon [cerebralvalley.ai](https://cerebralvalley.ai/e/meta-ai-developer-hackathon) dan dokumentasi resmi [dev.meta.ai](https://dev.meta.ai/docs/models.md). Dibaca langsung, bukan dari ringkasan pihak ketiga, kecuali dicatat.

**Hackathon**
- Indie track dinilai pada **kreativitas, ambisi teknis, dan apakah benar-benar berfungsi**. Artinya demo harus jalan, dan kedalaman pemakaian API dihitung.
- Tema "Build for Your World" cocok persis dengan ceritamu.
- **Official Rules baru diterbitkan sebelum 26 Okt**, dan halaman tidak menyebut apakah kode sebelum periode boleh. Jadi: tidak ada kode sebelum rules dibaca. Persiapan data dan riset aman.
- Kredit yang tertulis di halaman hanya kredit partner (Baseten/Modal/Fireworks, untuk peserta tertentu, model open-weights). **Angka "$150 kredit Meta" dari IDEA.md tidak kutemukan di halaman**; mungkin ada di email penerimaan. Cek.

**Meta Model API** (`https://api.meta.ai/v1`, kompatibel OpenAI SDK; kunci dibuat di dev.meta.ai)
- **Model:** `muse-spark-1.3` (direkomendasikan; input teks/gambar/video/audio/PDF; konteks 1M token). Untuk input audio ke Spark pakai `muse-spark-1.2`. `muse-wonder-1.0` (preview, reasoning lebih kuat, $4/$20 per 1M token) tersedia sebagai opsi.
- **Gambar:** `image_url` (URL publik atau data URL base64), maks 50 MB, JPEG/PNG/WebP, ±1.300–1.500 token per gambar. Gambar harus di pesan `user`.
- **Structured output:** didukung. Chat Completions: `response_format: {type: "json_schema"}`; Responses API: `text.format`. Skema rekursif tidak didukung, maks 10 level bersarang. Gambar + structured output dipakai bersama, tapi dokumen tidak mencatat batasannya, jadi **diuji hari 1**.
- **Instruksi:** pakai pesan `developer`, bukan `system`. `reasoning_effort` dari `minimal` sampai `xhigh` (token reasoning ditagih sebagai output; pakai rendah untuk ekstraksi).
- **Harga Spark (standard):** $1,25 input / $4,25 output per 1M token. Satu struk ≈ 1,5 ribu token gambar + output JSON ⇒ **kurang dari 1 sen per struk**; biaya bukan kendala.
- **Rate limit:** 3.000 RPM / 4M TPM per tim. Lebih dari cukup.
- **Muse Voice Transcribe** (`muse-voice-transcribe-1.0`, $0,18/jam): **bahasa Indonesia didukung** (25 bahasa, termasuk code-switching). Ada endpoint file `POST /v1/asr/transcribe` (WAV mono 16-bit, 16/24 kHz, maks 10 menit/32 MB) dan WebSocket realtime. Rekaman browser (webm) harus dikonversi ke WAV; WebSocket butuh kunci di server (proxy), jangan di klien.
- **Embedding: tidak ada** di daftar model Meta API. Rencana: shortlist `pg_trgm` (+ opsional embedding open-source di pgvector) → Muse Spark memilih produk kanonik. Tidak perlu training.
- **Lain-lain yang berguna:** `web_search` (grounding, $2,50/1.000 query) untuk produk yang tidak dikenal; `sam-3.1` untuk segmentasi gambar (kandidat untuk label rak, stretch).

**Risiko yang harus dicek SEGERA (sebelum 26 Okt)**
1. **Akses dari Indonesia.** Geographic Use Policy tidak menyebut Indonesia sebagai wilayah terlarang, tapi juga tidak mengonfirmasi "sudah diaktifkan". Beberapa laporan pihak ketiga (Juli–Agustus 2026) menyebut preview hanya untuk AS, dan dokumen blog Meta menyebut "open to developers in the US". Karena kamu sudah diterima, kemungkinan akun hackathon punya akses, tapi **buat akun/kunci di dev.meta.ai dan panggil `GET /v1/models` hari ini**. Kalau gagal, hubungi Discord/office hours panitia sebelum 26 Okt.
2. **Dukungan bahasa Indonesia untuk Muse Spark** (teks/vision) tidak didokumentasikan. Uji pada struk Kudus di hari 1.
3. **Akurasi ekstraksi struk termal** (nama terpotong) belum terbukti. Itu tujuan hari 1–2.

**Ide ambisi teknis yang murah**
- **Kaskade model:** struk diekstraksi `muse-spark` dengan reasoning rendah; baris berkonfiden rendah dieskalasi ke `muse-wonder` atau reasoning tinggi sebelum jatuh ke layar verify. Ukur: biaya vs. akurasi vs. jumlah koreksi manual. Ini cerita "ambisi teknis" yang bisa dibuktikan dengan angka.

## Pertanyaan terbuka

- Nama toko A/B/C dan tipe masing-masing.
- Apakah akun Meta Model API dari Kudus bisa dibuat dan mengembalikan model (`GET /v1/models`)? Berapa kredit yang sebenarnya diberikan?
- Isi Official Rules (kode sebelum periode, kepemilikan, syarat video/repo) saat terbit 26 Okt.
