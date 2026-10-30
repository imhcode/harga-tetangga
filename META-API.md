# Peta Pengetahuan Meta Model API untuk HargaTetangga

Dibaca langsung dari [dev.meta.ai](https://dev.meta.ai/) pada 22 Okt 2026 (±40 halaman dari ±150 halaman dalam indeks [llms.txt](https://dev.meta.ai/llms.txt)). Setiap klaim di bawah berasal dari halaman yang disebut. Yang **tidak** dijelaskan dokumen ditandai **[BELUM TERBUKTI]** dan masuk daftar uji hari 1 (bagian 9).

Catatan: ringkasan halaman dihasilkan model kecil, jadi angka kritis (harga, batas, ketentuan hukum) sebaiknya dicek ulang di sumber sebelum dipegang.

---

## 1. Lembar contekan (satu halaman)

| Hal | Nilai |
|---|---|
| Base URL | `https://api.meta.ai/v1` (Anthropic Messages: host `https://api.meta.ai`, tanpa `/v1`) |
| Autentikasi | `Authorization: Bearer $MODEL_API_KEY`. Kunci berawalan `LLM_`, hanya tampil sekali saat dibuat |
| SDK | **OpenAI SDK** (Python `pip install openai`, TS `npm install openai`) dengan `base_url`. SDK tidak membaca `MODEL_API_KEY` otomatis, kirim `api_key` eksplisit |
| Model utama | `muse-spark-1.3` (direkomendasikan untuk pekerjaan baru), `1.2` bila perlu audio input, `1.1` hanya mode Standard |
| Model kuat | `muse-wonder-1.0` (preview; $4 input / $20 output per 1M token; tidak ada mode Contributor) |
| Suara | `muse-voice-transcribe-1.0`, `POST /v1/asr/transcribe` (file) atau WebSocket realtime |
| Segmentasi | `sam-3.1` lewat `/v1/responses` (kotak dan mask, tidak membaca teks) |
| Cek akses | `GET /v1/models` (model yang diaktifkan untuk timmu), `GET /v1/status` (kesehatan layanan, tanpa auth) |
| Harga Spark (Standard) | $1,25 input / $4,25 output per 1M token; input ter-cache $0,15; tanpa premi konteks panjang |
| Harga Spark (Contributor) | $0,10 / $0,20, **tapi datamu dipakai melatih model Meta** (bagian 6) |
| Voice | $0,18 per jam audio, dihitung per detik |
| Rate limit | Per **tim** (bukan per kunci): Spark Standard 3.000 RPM / 4M TPM; Contributor 150 RPM / 3M TPM; Voice 256 stream bersamaan, 16.000 stream/jam |
| Konteks | 1.048.576 token |
| Sumber kebenaran | [Models](https://dev.meta.ai/docs/models.md), [Pricing](https://dev.meta.ai/docs/pricing-rate-limits.md) |

### Kerangka permintaan ekstraksi struk (sesuai dokumen, bukan kode proyek)

Chat Completions, tanpa status di server, gambar sebagai data URL:

```json
{
  "model": "muse-spark-1.3",
  "reasoning_effort": "low",
  "max_completion_tokens": 4000,
  "messages": [
    {"role": "developer", "content": "<instruksi + katalog produk, bagian STABIL ditaruh pertama>"},
    {"role": "user", "content": [
      {"type": "text", "text": "Ekstrak struk ini."},
      {"type": "image_url", "image_url": {"url": "data:image/jpeg;base64,..."}}
    ]}
  ],
  "response_format": {"type": "json_schema",
    "json_schema": {"name": "receipt", "strict": true, "schema": {"...": "..."}}}
}
```

Pada Responses API parameternya berbeda: `reasoning.effort`, `text.format` (bukan `response_format`; mengirim `response_format` ke Responses memberi 400), dan blok `input_image`.

---

## 2. Peta kebutuhan proyek ke fitur Meta

| Kebutuhan HargaTetangga | Fitur Meta | Halaman | Catatan dan risiko |
|---|---|---|---|
| Foto struk → JSON | Vision + structured output | [image-understanding](https://dev.meta.ai/docs/image-understanding.md), [structured-output](https://dev.meta.ai/docs/structured-output.md) | Gambar JPEG/PNG/WebP, maks 50 MB inline, ≤50 gambar/permintaan. Kombinasi gambar + skema tidak dijelaskan batasannya **[BELUM TERBUKTI]** |
| Pencocokan nama struk → produk kanonik | Penalaran model + daftar kandidat dalam prompt | [reasoning](https://dev.meta.ai/docs/reasoning.md) | **Tidak ada model embedding.** Shortlist `pg_trgm`, model memilih |
| Daftar belanja lisan | Muse Voice Transcribe | [speech-to-text](https://dev.meta.ai/docs/speech-to-text.md) | Bahasa Indonesia didukung, ada code-switching |
| Parsing daftar belanja, "masak apa?" | Teks + structured output | [structured-output](https://dev.meta.ai/docs/structured-output.md) | Tool calling hanya `tool_choice: auto`; untuk keluaran terkontrol pakai structured output |
| Penjelasan rekomendasi | Teks biasa | – | Model selalu bernalar; jaga `reasoning_effort` rendah agar cepat |
| Kaskade akurasi | Spark → Wonder untuk baris ragu | [models](https://dev.meta.ai/docs/models.md) | Wonder preview dan mahal; ukur biaya vs akurasi |
| Label rak (stretch) | Spark vision membaca label; SAM menemukan "price tag" | [sam/overview](https://dev.meta.ai/docs/sam/overview.md) | SAM hanya kotak/mask, **tidak membaca teks**. Cukup Spark dulu |
| Info produk tak dikenal | Search grounding | [search-grounding](https://dev.meta.ai/docs/search-grounding.md) | Hanya Responses/Messages, $2,50/1.000 query, tidak ada jaminan bisa ambil harga toko. Rendah prioritas |
| Privasi di perangkat (visi) | Muse Glimmer (open-weight, 30B, Apache 2.0, input gambar) | [muse-glimmer](https://dev.meta.ai/docs/muse-glimmer.md) | Butuh GPU besar; tidak realistis 10 hari. Hanya untuk narasi visi |

---

## 3. Aturan dan jebakan teknis (yang bisa memakan waktu)

**Model dan parameter**
- Muse Spark **selalu bernalar**. `reasoning_effort: "none"` → HTTP 400. Nilai sah: `minimal`, `low`, `medium`, `high`, `xhigh` (`max` hanya Spark 1.3 Standard dan Wonder). Untuk ekstraksi mulai dari `low`.
- Token penalaran ditagih sebagai output dan memakan `max_completion_tokens`. **Beri batas besar**, atau JSON bisa terpotong.
- Tidak didukung (400): `stop`, `logit_bias`, `prediction`, `verbosity`, `n>1`, `logprobs`, `top_p: 0`, `modalities`, `audio`. Biarkan `temperature` di 1,0.
- Pakai role `developer` untuk instruksi (`system` diterima dan disamakan).
- Gambar hanya boleh di pesan `user`, selain itu diabaikan atau 400.
- Tool calling: hanya `tool_choice: "auto"`; hasil argumen tetap harus divalidasi sendiri.

**Structured output**
- Chat Completions: `response_format`; Responses: `text.format`.
- Skema rekursif ditolak (400). Batas: 10 level, 5.000 properti. Pakai `additionalProperties: false` dan semua field di `required`.
- **"Skema menjamin bentuk, bukan fakta"** (resep chart-analysis). Rancang skema dengan field nullable, daftar `unreadable`, dan sumber baca per nilai, lalu verifikasi manusia di layar Review & Verify. Itu persis arsitektur kita.
- Cara menandai field opsional/nullable dalam skema **tidak dijelaskan** **[BELUM TERBUKTI]**; uji `["number","null"]`.

**Status dan penyimpanan**
- **Responses API menyimpan respons secara default (`store: true`).** Untuk data pribadi pakai `store: false`, atau cukup pakai Chat Completions (tanpa status percakapan di server; Meta tetap menyimpan Content dan Usage Data sesuai §4.4 ToS, jadi ini bukan jaminan tidak ada retensi).
- **Files API menyimpan file sampai dihapus** (tidak kedaluwarsa kecuali diatur; batas 100 GiB per tim). Kirim gambar **inline base64** (tidak disimpan) dan hindari Files API.
- Pengiriman inline maks 50 MB; kompres foto sisi klien ±1.280 px (±1.300–1.500 token per gambar). Hitung biaya nyata dengan `POST /v1/responses/input_tokens` pada contoh struk sungguhan; **tidak ada rumus resmi**.

**Latensi dan kegagalan**
- Permintaan non-streaming yang terlalu lama → 504 (jangan di-retry sama persis). Pakai `stream: true` atau kurangi input/`reasoning_effort`/output.
- Retry: 429, 500, 503 (backoff eksponensial + jitter, 3–5 kali, hormati `Retry-After`). Jangan retry 400/401/402/403/404/413.
- **402 `billing_not_configured`** muncul jika metode pembayaran belum dipasang.
- Header `x-ratelimit-*` ada pada respons sukses, untuk melambat sebelum kena 429.
- Cek `GET /v1/status` sebelum memutuskan retry saat gangguan.

**Prompt caching**
- Otomatis, berbasis awalan. Taruh bagian **stabil dulu** (instruksi, katalog, contoh), bagian yang berubah (gambar, teks pengguna) terakhir. Baca `usage.prompt_tokens_details.cached_tokens`. Panduan tentang gambar dalam cache tidak ada **[BELUM TERBUKTI]**.
- `prompt_cache_key` stabil untuk beban tinggi (jangan per pengguna).

**Suara**
- Endpoint file: WAV **mono, 16-bit PCM, 16 atau 24 kHz**, maks 10 menit / 32 MB. Rekaman browser (webm/ogg) harus dikonversi; contoh resmi memakai ffmpeg. Di browser, encode WAV sisi klien (Web Audio API) lalu kirim ke route server kita.
- WebSocket realtime butuh kunci di frame handshake pertama, jadi harus lewat proxy server (jangan dari browser). Untuk daftar belanja pendek, **endpoint file + mode `PUSH_TO_TALK` (tombol mik) sudah cukup**.
- `languageBias`: daftar nama bahasa, mis. `["Indonesian"]`. Hanya petunjuk. Ada parameter `keywords` untuk istilah domain (nama produk); nama field persisnya **[BELUM TERBUKTI]**, cek di [skema voice](https://dev.meta.ai/docs/api-reference/voice/schemas.md).
- Tidak ada panduan khusus bahasa Indonesia, derau, atau ucapan sangat pendek **[BELUM TERBUKTI]**.

**Lain-lain**
- Kunci tidak boleh ada di kode browser/mobile; proksi lewat server. Kunci terpisah per aplikasi. Jangan commit.
- Mode Contributor tidak punya fitur `max` reasoning dan terbatas 150 RPM.
- Dokumen tidak konsisten di beberapa tempat (mis. status endpoint disebut tanpa auth tapi contoh memakai Bearer; blog masih memakai `muse-spark-1.1`). Percayai `GET /v1/models` untuk daftar model aktif.

---

## 4. Biaya (estimasi kasar, asumsi dicantumkan)

Asumsi: 1 struk ≈ 1.500 token gambar + ±3.000 token instruksi/katalog + ±2.300 token output (termasuk penalaran), Spark Standard, tanpa cache. **Angka ini asumsi, bukan terukur.**

| Pekerjaan | Perkiraan |
|---|---|
| 1 struk | ±$0,015 (sekitar 1,5 sen USD) |
| 30 struk × 10 kali percobaan | ±$4,5 |
| 1 daftar belanja suara 15 detik | ±$0,001 |
| Penjelasan rekomendasi (teks) | <$0,005 |

Biaya jelas bukan kendala. Kendalanya akses, bukan uang.

---

## 5. Akses, akun, dan penagihan

- Daftar: dev.meta.ai → "Start building" → "Individual". Metode: email/nomor HP atau Facebook/Google/GitHub. **Harus 18+ dan tinggal di negara yang didukung.** Pendaftaran membuat akun + tim.
- **Sebelum request pertama, kamu harus menambahkan metode pembayaran (kartu) dan membuat kunci API.** Halaman bantuan penagihan juga meminta nama dan alamat bisnis. Mata uang dan apakah kartu Indonesia diterima **tidak dijelaskan**. Siapkan kartu debit/kredit/virtual yang bisa bertransaksi internasional (USD).
- Penagihan berbasis ambang: saldo ditagih saat mencapai threshold, sisa ditagih tanggal 1 tiap bulan. **Atur spend limit alert** ([help](https://dev.meta.ai/help/billing/spend-limit-alert)).
- Kredit gratis atau kredit hackathon: **tidak disebut di dokumen mana pun yang kubaca**. Pastikan lewat email penerimaan atau panitia.
- **Negara:** halaman bantuan: tersedia di "sebagian besar negara tempat Meta AI tersedia dan penagihan didukung". [Geographic Use Policy](https://dev.meta.ai/legal/geographic-use-policy) hanya mendaftar wilayah **terlarang** (Indonesia tidak termasuk), dan tidak mendaftar negara yang diaktifkan. Blog Meta masih menulis "preview untuk developer di AS". Jadi **akses Indonesia belum terkonfirmasi dan hanya bisa dibuktikan dengan mencoba mendaftar**.

---

## 6. Hukum dan privasi: dampaknya ke desain

Dari [Terms of Service](https://dev.meta.ai/legal/terms-of-service) dan [Acceptable Use Policy](https://dev.meta.ai/legal/acceptable-use-policy) (ringkasan model; bukan nasihat hukum):

| Ketentuan | Dampak untuk kita |
|---|---|
| Mode **Standard**: Meta "tidak memakai Content untuk melatih model" (§5.1). Mode **Contributor**: Meta boleh memakai Content untuk melatih dan mengevaluasi, dan "tidak ada mekanisme untuk mengecualikan traffic tertentu" (§6) | **Semua traffic pengguna/struk asli lewat model Standard** (`muse-spark-1.3`, bukan `-contributor`). Contributor hanya boleh untuk data uji tanpa info pribadi |
| Dilarang mengirim info sensitif/rahasia/pribadi ke Contributor (§6.2) | Struk berisi nama, nomor member, 4 digit kartu → **jangan** ke Contributor. Tetap lakukan minimisasi: crop atau tutup bagian itu sebelum unggah |
| Wajib punya dan menaati **kebijakan privasi** untuk Integrated Product (§6.2) | Siapkan halaman kebijakan privasi sederhana di aplikasi demo |
| Pengguna dan kamu harus 18+, produk tidak boleh menyasar di bawah 18 (§10.1) | Cantumkan di syarat penggunaan/onboarding |
| Kunci API tidak boleh dibagikan; pengguna hanya boleh berinteraksi lewat produkmu (§3.1, §10.1(i)) | Semua panggilan lewat route server kita; tidak ada kunci di klien |
| Wajib menyampaikan batasan/risiko AI dan label konten hasil AI jika hukum mewajibkan (§1, AUP) | Layar Review & Verify sekaligus pernyataan "diekstraksi AI, periksa dulu" |
| Dilarang memakai output untuk melatih/mendistilasi model pesaing Meta (§10.1(ix)); evaluasi sistem sendiri diperbolehkan | **Jangan** memakai output Spark untuk melatih model lain. Mengukur akurasi sendiri boleh |
| AUP melarang "praktik keuangan/pajak tanpa lisensi" | Posisikan sebagai pencatat dan pembanding harga, **bukan** penasihat keuangan. Tidak ada saran investasi |
| §1: Layanan "ditujukan untuk penggunaan komersial, kecuali Meta secara tegas menyediakan untuk penggunaan personal/konsumen" | Aplikasi konsumen buatan individu: **konfirmasi ke panitia/Meta** apakah ini dalam cakupan |
| ZDR (zero data retention) hanya untuk akun "qualified" lewat sales; menonaktifkan Files, search, background, Contributor | Tidak relevan untuk hackathon; sebutkan sebagai langkah produksi |
| Standar masa simpan data input di mode Standard: **tidak ada angkanya** di halaman yang kubaca | Jangan menjanjikan angka retensi di kebijakan privasi kita; rujuk kebijakan Meta |

Kepemilikan: "kamu memiliki seluruh hak atas Input dan Output" (§5.1/§4.1); Meta tidak mengklaim hak cipta atas output.

---

## 7. Opsi ambisi teknis (nilai "technical ambition", urut dari yang murah)

1. **Kaskade model Spark → Wonder** (murah, terukur): ekstraksi `muse-spark-1.3` dengan `reasoning_effort` rendah, baris berkonfiden rendah dieskalasi ke Wonder/reasoning tinggi. Ukur biaya, akurasi, jumlah koreksi manusia.
2. **Pola "tanya sebelum kirim"** dari [cookbook Glimmer](https://dev.meta.ai/docs/cookbook/glimmer-api-agent.md): apa yang keluar dari perangkat ditampilkan dan disetujui pengguna, dengan log egress. Bisa ditiru untuk privasi (layar yang menunjukkan bagian struk yang dikirim), tanpa menjalankan Glimmer.
3. **Pengecekan konsistensi** ala resep chart-analysis: minta model menyebut dari mana tiap nilai dibaca, lalu validasi deterministik (jumlah item vs total struk, harga per satuan masuk akal). Itu bukti "bekerja" untuk juri.
4. **SAM + Spark untuk label rak** (stretch): SAM menemukan kotak label, Spark membaca teksnya.
5. **Muse Glimmer lokal**: hanya narasi visi. Dokumen hanya memverifikasi GPU server besar (bf16); laptop/ponsel tidak diverifikasi. Tidak realistis dalam 10 hari.

---

## 8. Halaman yang sengaja tidak kubaca

Muse Code (agen koding CLI; bisa dipakai membangun, tapi tidak mengubah desain), Muse Spark Open dan post-training, computer use, agent frameworks, tool search, image generation (Muse Image), video understanding, cookbook game/GitHub/slides. Alasan: tidak berkaitan dengan struk, suara, atau rekomendasi. Repositori contoh resmi: `github.com/meta-models/meta-model-cookbook` (disebut dokumen; belum kubuka).

---

## 9. Daftar uji hari 1 (semua **[BELUM TERBUKTI]**)

1. Akses: akun dibuat dari Kudus? `GET /v1/models` mengembalikan apa? Kartu Indonesia diterima?
2. Gambar struk termal + `json_schema` strict **bersamaan** (cocokkan ke skema; cara menandai nullable).
3. Akurasi membaca struk berbahasa Indonesia (nama terpotong, huruf kapital, singkatan) pada ±30 struk ground truth.
4. Struk **panjang dan sempit**: apakah perlu dipotong menjadi beberapa gambar? Hitung token dengan `/v1/responses/input_tokens`.
5. `reasoning_effort`: `minimal` vs `low` vs `medium` → akurasi, latensi, biaya.
6. Efek prompt caching dengan katalog panjang dan gambar di akhir (`cached_tokens`).
7. Voice: WAV 24 kHz dari browser; akurasi daftar belanja Indonesia; nama field `keywords`; `languageBias: ["Indonesian"]`.
8. Latensi total (foto → hasil) pada koneksi seluler; perlu `stream: true`?
9. Retry/backoff: simulasikan 429/503 dan 504.
10. Apakah `store: false` pada Responses menghilangkan penyimpanan seperti yang diharapkan (jika memakai Responses).

---

## 10. Dampak konkret pada rencana

1. **Tugas hari ini bertambah:** pasang metode pembayaran dan atur spend limit alert (tanpanya request pertama gagal dengan 402).
2. Ekstraksi memakai **Chat Completions, tanpa status, gambar inline**, model **Standard**. Tidak memakai Files API.
3. Semua panggilan Meta lewat **route server**, kunci hanya di environment variable server.
4. Tambah ke daftar kerja: halaman **kebijakan privasi** + batas **18+** + label "diekstraksi AI".
5. Voice: encode WAV 24 kHz sisi klien, endpoint file, mode `PUSH_TO_TALK`.
6. Akses Indonesia tetap risiko #1. Bukti hanya dari percobaan daftar nyata.
7. Cakupan "penggunaan konsumen" (§1 ToS) ditanyakan sekalian ke panitia.

---

## Indeks tautan penting

[Models](https://dev.meta.ai/docs/models.md) · [Pricing](https://dev.meta.ai/docs/pricing-rate-limits.md) · [Quickstart](https://dev.meta.ai/docs/quickstart.md) · [Authentication](https://dev.meta.ai/docs/authentication.md) · [SDKs](https://dev.meta.ai/docs/sdks.md) · [Choosing an API](https://dev.meta.ai/docs/protocols.md) · [Chat Completions](https://dev.meta.ai/docs/protocols/chat-completions.md) · [Responses](https://dev.meta.ai/docs/protocols/responses.md) · [Image understanding](https://dev.meta.ai/docs/image-understanding.md) · [Structured output](https://dev.meta.ai/docs/structured-output.md) · [Reasoning](https://dev.meta.ai/docs/reasoning.md) · [Prompt caching](https://dev.meta.ai/docs/prompt-caching.md) · [Token counting](https://dev.meta.ai/docs/token-counting.md) · [File handling](https://dev.meta.ai/docs/file-handling.md) · [Error handling](https://dev.meta.ai/docs/error-handling.md) · [Speech to text](https://dev.meta.ai/docs/speech-to-text.md) · [Transcribe API](https://dev.meta.ai/docs/api-reference/voice/transcribe.md) · [Status](https://dev.meta.ai/docs/api-reference/status.md) · [Chart analysis](https://dev.meta.ai/docs/cookbook/perception-chart-analysis.md) · [Terms](https://dev.meta.ai/legal/terms-of-service) · [AUP](https://dev.meta.ai/legal/acceptable-use-policy) · [Geographic policy](https://dev.meta.ai/legal/geographic-use-policy) · [Help Center](https://dev.meta.ai/help)
