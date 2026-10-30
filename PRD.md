# PRD HargaTetangga v1.0

Status: draf hasil sesi tanya-jawab 27 Okt 2026. Melengkapi [PLAN.md](PLAN.md) (jadwal dan keputusan awal) dan [META-API.md](META-API.md) (fakta API). Bila ada selisih, PRD ini yang terbaru.

Penulis: pemilik proyek (solo builder, ±4 jam/hari). Hackathon: Meta Global AI Developer Hackathon 2026, build 26 Okt – 4 Nov, submit 4 Nov 9 PM ET.

Tanda **[USULAN]** berarti isinya usulan dari sesi ini yang belum kamu putuskan. Tanda **[RISIKO]** berarti ada konflik dengan waktu atau fakta yang sudah dikumpulkan.

---

## 1. Ringkasan

HargaTetangga adalah aplikasi Android untuk rumah tangga yang belanja kebutuhan pokok. Aplikasi membaca struk lewat foto, mengingat harga tiap barang di tiap toko, lalu menjawab dua pertanyaan sebelum berangkat belanja: **berapa uang yang perlu dibawa** dan **di toko mana belanjanya paling hemat**.

Masalah yang diselesaikan bukan hanya harga yang berbeda antar toko. Tanpa harga terakhir di kepala, orang menebak uang yang dibawa. Bila membawa lebih, uang itu cenderung terpakai untuk barang yang tidak direncanakan.

## 2. Cerita pemicu

> Barang yang sama dijual di toko berbeda dengan harga berbeda, dan selisihnya terasa di dompet. Kami juga sering lupa harga terakhir, jadi menebak-nebak mau membawa uang lebih atau kurang. Kalau membawa lebih, biasanya jadi mengambil ini-itu yang tidak ada dalam rencana awal.

Sumber: jawaban pemilik proyek, 27 Okt 2026. Detail kejadian nyata (toko, tanggal, selisih rupiah) belum ditulis. **Lengkapi sebelum video demo**, karena cerita ini membuka demo.

## 3. Pengguna

| | |
|---|---|
| Pengguna utama v1.0 | Satu rumah tangga: pemilik proyek, istri, dan anggota keluarga lain yang diundang |
| Pengguna pertama | Istri, belanja kebutuhan pokok 2–3 kali seminggu |
| Lokasi uji | Kudus |
| Toko yang dicakup | Minimarket (Indomaret, Alfamart), supermarket/grosir, warung dan tukang sayur |
| Di luar cakupan toko | Pasar tradisional (tidak dipilih untuk 1.0) |
| Batas usia | 18+ (syarat Meta ToS §10.1) |

Daftar nama toko uji masih terbuka (lihat bagian 13).

## 4. Tujuan dan non-tujuan

### Tujuan

1. Pengguna mendapat **estimasi uang yang perlu dibawa** untuk sebuah daftar belanja, dengan rentang dan penjelasan.
2. Pengguna mendapat **rekomendasi toko** (satu toko, atau kombinasi dua) beserta potensi hemat dalam rupiah.
3. Data harga masuk dengan **usaha minimal**: foto struk, koreksi hanya pada baris yang ragu.
4. Model AI dipakai **di banyak titik dan terukur**, karena kedalaman pemakaian API adalah kriteria juri.
5. Dampak terbukti pada **belanja nyata** keluarga sendiri.

### Non-tujuan (v1.0)

- Peta dan jarak. Diganti biaya tetap per toko tambahan (default Rp 3.000, bisa diubah).
- Optimizer sayur dan lauk segar.
- Lingkaran tetangga, berbagi harga dengan orang luar keluarga, dan promo. Hanya visi.
- Foto label rak dan SAM. Hanya jika waktu sisa (stretch).
- Melatih atau menyetel model. Pakai katalog kanonik, tabel alias, dan model sebagai pemilih.
- Saran keuangan. Aplikasi mencatat dan membandingkan harga (AUP Meta melarang praktik keuangan tanpa lisensi).
- Pasar tradisional.
- Notifikasi harga naik.

## 5. User story

| ID | Sebagai | Aku ingin | Supaya | Prioritas |
|---|---|---|---|---|
| U1 | Istri yang belanja | memotret struk lalu melihat daftar barang dan harganya | tidak perlu mengetik, dan harga terakhir tersimpan | P0 |
| U2 | Istri | mengoreksi hanya baris yang tidak yakin | verifikasi cepat dan alias belajar dari koreksiku | P0 |
| U3 | Istri | menyusun daftar belanja lalu melihat estimasi total dan rentangnya | aku tahu berapa uang yang dibawa | P0 |
| U4 | Istri | melihat toko termurah untuk daftar itu, termasuk biaya toko tambahan | aku memilih toko tanpa keliling cek harga | P0 |
| U5 | Istri | bilang "mau masak X" lalu mendapat daftar bahan, mencentang yang perlu dibeli dan melewati yang sudah ada di rumah | daftar belanjaku tidak kelebihan | P1 |
| U6 | Istri | mengucapkan daftar belanja | aku tidak perlu mengetik di HP saat sibuk | P1 |
| U7 | Pemilik rumah tangga | mengundang anggota keluarga dan menyetujui permintaannya | data harga dan struk dibagi dalam keluarga | P1 |
| U8 | Pengguna | bertanya "kapan terakhir beli minyak dan berapa?" | aku tidak perlu membuka riwayat satu per satu | P1 |
| U9 | Pemilik rumah tangga | melihat laporan bulanan dan menyaring barang yang penting saja | aku tahu ke mana uang belanja pergi | P1 |
| U10 | Pengguna | melihat bagian struk yang akan dikirim ke model dan menutup data pribadi | aku mengontrol apa yang keluar dari HP | P1 |
| U11 | Pengguna | melihat item struk yang tidak ada di daftar rencana | aku sadar belanja di luar rencana | P1 |

U11 dibutuhkan oleh metrik dampak "belanja di luar rencana turun" (bagian 9). Ia tidak kamu pilih secara eksplisit, jadi dibuat versi hitungan biasa tanpa AI.

## 6. Fitur dan kebutuhan fungsional

### 6.1 Foto struk → data harga (P0)

- Kamera (CameraX) mengambil foto, kompres sisi klien ±1.280 px.
- Permintaan ke Muse Spark lewat server (bagian 8): Chat Completions, gambar inline base64, `response_format` JSON schema strict, `reasoning_effort` rendah. Keluaran: toko, tanggal, item (nama mentah, jumlah, harga), total, dan daftar `unreadable`.
- **Kaskade:** baris berkonfiden rendah dieskalasi ke `muse-wonder-1.0` atau reasoning lebih tinggi. Catat biaya, akurasi, dan jumlah koreksi per tingkat.
- **Cek konsistensi:** model menyebut asal tiap nilai (teks yang dibaca). Validasi deterministik menandai baris bila jumlah item tidak cocok dengan total, harga per satuan tidak wajar, atau angka tidak terbaca.

### 6.2 Review & Verify dan alias (P0)

- Baris yakin diterima otomatis. Hanya baris yang ditandai dibuka untuk dikoreksi.
- Pemetaan nama struk ke produk kanonik: shortlist `pg_trgm`, Muse Spark memilih dari kandidat. Koreksi pengguna disimpan sebagai alias per toko (jumlah konfirmasi dan siapa yang mengonfirmasi).
- Label di layar: "Diekstraksi AI, periksa dulu".

### 6.3 Harga tersimpan (P0)

- Setiap observasi harga menyimpan produk, toko, harga, harga per satuan, tanggal, sumber (struk/suara/manual), dan status verifikasi.
- Setiap harga di layar menampilkan **umur** dan **sumber**. Barang tanpa data ditandai "belum ada data", tidak ditebak.

### 6.4 Daftar belanja dan agen belanja (P0 untuk teks dan estimasi)

- Daftar dibuat dengan mengetik. Daftar lewat suara masuk sebagai U6.
- **Agen dengan tool calling** (`tool_choice: auto`; argumen divalidasi sendiri). Tool awal: `cari_harga`, `hitung_estimasi`, `bandingkan_toko`, `riwayat_pembelian`, `buat_daftar_dari_menu`.
- Estimasi menampilkan total, **rentang** (berdasarkan umur dan variasi harga), dan angka "bawa sekian" dengan cadangan **[USULAN: cadangan 10%]**.
- Rekomendasi: enumerasi kombinasi 1 dan 2 toko. Biaya = harga termurah per barang + biaya tetap per toko tambahan.
- Agen menjelaskan hasil dengan bahasa ramah, dan menolak menebak barang tanpa data.

### 6.5 "Mau masak X" (P1)

- Pengguna menulis atau mengucapkan menu. Agen membuat daftar bahan lewat structured output, lalu setiap bahan **dicentang** untuk masuk daftar belanja, atau dilewati karena sudah ada di rumah.
- Bahan dipetakan ke produk di database. Yang belum ada ditandai "belum ada data".
- Fokus produk kemasan. Bahan segar hanya masuk daftar, tidak masuk optimizer.

### 6.6 Suara (P1)

- Rekam dengan `AudioRecord` (PCM 16-bit mono, 24 kHz), bungkus sebagai WAV, kirim ke endpoint file `POST /v1/asr/transcribe` lewat server. Mode tombol tahan bicara (`PUSH_TO_TALK`).
- Teks hasil transkripsi diubah Spark menjadi item daftar belanja. `languageBias: ["Indonesian"]`.

### 6.7 Tanya-jawab riwayat (P1)

- Pertanyaan bebas atas data sendiri ("kapan terakhir beli minyak?"). Dijawab oleh agen yang sama dengan tool `riwayat_pembelian`. Tidak ada tool baru.

### 6.8 Laporan bulanan (P1)

- Tabel dan grafik biasa, dihitung dari database, tanpa AI.
- Pengguna menandai barang atau kategori sebagai "penting". Laporan bisa disaring ke yang penting saja.

### 6.9 Perbandingan rencana vs struk (P1)

- Setelah struk diverifikasi, item yang tidak ada di daftar rencana ditandai "di luar rencana". Dihitung biasa, tanpa AI, dan netral nadanya. Menjadi bahan metrik dampak.

### 6.10 Tanya sebelum kirim (P1)

- Sebelum foto dikirim, layar menunjukkan apa yang akan dikirim dan memberi opsi menutup (crop atau blok) nama, nomor member, dan 4 digit kartu. Dicatat dalam log egress yang bisa dilihat pengguna. Meniru pola cookbook Glimmer tanpa menjalankan Glimmer.

### 6.11 Akun dan keluarga (P1, versi minimal)

- Login sederhana (Supabase Auth).
- Pemilik rumah tangga membuat **kode undangan**. Anggota baru memasukkan kode, pemilik menekan **Setuju**. Setelah itu data harga dan struk dibagi dalam rumah tangga.
- Tidak ada peran bertingkat, izin per fitur, atau pengelolaan anggota lanjutan di v1.0.
- Struk mentah dan foto hanya terlihat oleh pengunggah. Observasi harga terlihat oleh rumah tangga.

## 7. Pemakaian model AI

| Titik | Model dan fitur | Dibuktikan dengan |
|---|---|---|
| Baca struk | `muse-spark-1.3` vision + structured output | % baris benar otomatis, waktu verifikasi |
| Kaskade | `muse-wonder-1.0` untuk baris ragu | Biaya vs akurasi vs jumlah koreksi |
| Cek konsistensi | Asal nilai dari model + validasi deterministik | Jumlah kesalahan yang tertangkap sebelum Review |
| Pencocokan alias | `pg_trgm` shortlist, Spark memilih | % pemetaan otomatis benar, tren seiring alias bertambah |
| Agen belanja | Tool calling + reasoning | Akurasi estimasi vs total kasir |
| Daftar dari menu | Teks + structured output | Daftar bahan yang diterima tanpa diubah |
| Suara | `muse-voice-transcribe-1.0` + Spark | Akurasi daftar belanja lisan |

Aturan wajib dari [META-API.md](META-API.md):

- Semua traffic pengguna memakai model **Standard** (Contributor melatih model Meta dan melarang data pribadi).
- Chat Completions tanpa status, gambar inline. Tidak memakai Files API.
- `reasoning_effort: "none"` ditolak (400). Beri `max_completion_tokens` besar karena token penalaran ikut terhitung.
- Tidak ada embedding. Jangan memakai keluaran Spark untuk melatih model lain.

## 8. Arsitektur

| Lapisan | Pilihan | Catatan |
|---|---|---|
| Aplikasi | Android native, Kotlin + Jetpack Compose | CameraX, `AudioRecord`, penyimpanan lokal untuk draf |
| Backend | Supabase | Auth, Postgres dengan `pg_trgm`, Edge Functions sebagai proxy ke Meta API |
| Kunci API | Hanya di environment Edge Functions | Tidak pernah ada di aplikasi |
| Model | Meta Model API `https://api.meta.ai/v1` | Standard mode |

Model data (ringkas, dari PLAN.md, ditambah rumah tangga): `households`, `household_members`, `household_invites`, `stores`, `products`, `product_aliases`, `receipts` (hanya pemilik), `price_observations`, `shopping_lists`, `shopping_list_items`, `product_priority` (penanda "penting").

Kebijakan privasi sederhana dan batas 18+ ditampilkan di onboarding. Jangan menjanjikan angka retensi data Meta.

## 9. Metrik keberhasilan

| Metrik | Target | Cara ukur |
|---|---|---|
| Baris struk benar otomatis | **≥85%** (ambang pivot di PLAN.md: <60%) | ±30 struk ground truth, lalu struk nyata |
| Akurasi estimasi vs total kasir | Median selisih ≤10% **[USULAN]** | Estimasi sebelum berangkat vs total struk, per trip |
| Belanja di luar rencana turun | Turun dibanding baseline **[USULAN: baseline 1–2 minggu tanpa app]** | Persentase item tak terencana dari U11 |
| Waktu belanja hemat | Menit per trip **[USULAN: lapor sendiri istri]** | Catatan sederhana setelah trip |
| Potensi hemat keranjang | Rp dan % **[USULAN]** | Eksperimen 15 barang × 4–5 toko, dipisahkan dari hemat yang benar-benar terjadi |
| Waktu verifikasi per struk | Turun seiring alias bertambah | Dicatat per struk |

Semua angka dicatat sejak hari pertama uji. Dampak ke juri berupa angka dan cerita, bukan klaim.

## 10. Prioritas dan batas potong

**P0 (harus jalan untuk demo):** foto struk → data harga (6.1), Review & Verify dan alias (6.2), harga tersimpan (6.3), daftar belanja teks dan agen estimasi (6.4).

**P1 (urutan potong dari bawah bila waktu habis):**

1. Tanya sebelum kirim (6.10)
2. Laporan bulanan (6.8)
3. Tanya-jawab riwayat (6.7)
4. Suara (6.6)
5. Akun dan keluarga (6.11)
6. "Mau masak X" (6.5)
7. Perbandingan rencana vs struk (6.9). **Bagian terakhir yang dipotong**, karena metrik dampak bergantung padanya.

**Stretch:** foto label rak (SAM + Spark).

**Gerbang beku fitur:** akhir D8 (Sen 2 Nov). Setelah itu hanya polish, video, README.

## 11. Risiko

| Risiko | Dampak | Penanganan |
|---|---|---|
| **[RISIKO]** Akses Meta API dari Indonesia belum terkonfirmasi | Seluruh fitur AI tidak jalan | Hari ini: `GET /v1/models` dari Kudus; bila gagal, hubungi panitia |
| **[RISIKO]** Kotlin + Compose untuk satu orang, ±40 jam, ditambah akun dan undangan keluarga | Fitur andalan tidak selesai | Kerjakan P0 dulu di satu akun, tambahkan akun/keluarga setelah alur inti jalan di HP; batas potong di bagian 10 |
| Akurasi baca struk Indonesia belum terbukti | Metrik utama meleset | Uji ±30 struk di D1–D2; bila <60%, perkuat UI Review dan persempit katalog |
| Estimasi uang memakai harga lama | Estimasi menyesatkan | Tampilkan umur harga dan rentang; barang tanpa data ditandai, tidak ditebak |
| Agen salah memanggil tool | Jawaban salah | Validasi argumen di server; jawaban selalu menyebut sumber harga |
| Struk berisi data pribadi | Pelanggaran privasi | Mode Standard saja, layar "tanya sebelum kirim", kebijakan privasi |
| Cakupan "penggunaan konsumen" di ToS Meta §1 | Kelayakan submission | Tanyakan ke panitia sekaligus dengan Official Rules |
| Edge Functions dan batas waktu respons | 504 pada struk panjang | `stream: true` atau kurangi `reasoning_effort`; jangan retry 504 persis sama |

## 12. Jadwal (disesuaikan dari PLAN.md)

> Diganti oleh [BACKLOG.md](BACKLOG.md) per 30 Okt 2026 (fase e2e, gerbang HITL, garis potong). Tabel di bawah disimpan sebagai riwayat.

Sekarang D2 (Sel 27 Okt). Perubahan terhadap PLAN.md hanya karena Android native dan akun/keluarga:

| Hari | Fokus |
|---|---|
| D1–D2 | Cek akses API; skema ekstraksi dan uji struk; kerangka proyek Android dan Supabase |
| D3 | Kamera → Edge Function → ekstraksi; layar Review & Verify; alias |
| D4 | Daftar belanja, mesin rekomendasi, agen dengan tool calling (estimasi) |
| D5 | Gerbang: alur inti jalan di HP. Suara dan "mau masak X" jika lancar |
| D6–D7 | Uji lapangan belanja nyata; catat metrik; perbaikan |
| D8 | Akun/keluarga minimal, laporan, riwayat; **fitur dibekukan** |
| D9 | Polish, video demo (+ cadangan), README |
| D10 (Rab 4 Nov) | Kirim sebelum 22:00 WIB |

## 13. Pertanyaan terbuka

1. Akun Meta Model API dari Kudus: bisa dibuat? Kartu Indonesia diterima? Kredit hackathon berapa?
2. Isi Official Rules (kode sebelum periode, kepemilikan, syarat video dan repo).
3. Nama dan tipe 4–5 toko uji.
4. Cerita kejadian nyata (toko, tanggal, selisih rupiah) untuk pembuka demo.
5. Target numerik yang bertanda **[USULAN]** di bagian 9, dan besar cadangan uang.
6. Apakah Kotlin + Compose tetap dipertahankan setelah D3, bila estimasi waktu P0 meleset.
