# Persiapan Hari-H: supaya tidak ada waktu kosong

Tujuan: saat hackathon mulai, **jam pertama sudah dipakai untuk membangun**, bukan mencari data, akun, atau keputusan. Semua yang tidak butuh kode diselesaikan sebelum 26 Okt.

> Aturan emas: **jangan menulis kode proyek sebelum Official Rules dibaca** (terbit sebelum 26 Okt, halaman acara belum menyebut aturan soal kode sebelum periode). Hal-hal di bawah ditandai **[AMAN]** (akun, data, desain di atas kertas, eksperimen manual) atau **[TUNGGU RULES]**.

## 1. Waktu nyata dalam WIB (periksa ulang di halaman acara)

- **Mulai:** Senin 26 Okt, 9:00 AM ET ≈ **20:00 WIB** (AS masih waktu musim panas).
- **Tenggat:** Rabu 4 Nov, 9:00 PM ET. AS pindah ke waktu standar pada 1 Nov, jadi ≈ **Kamis 5 Nov, 09:00 WIB**.
- **Target kirim:** Rabu 4 Nov **sebelum 22:00 WIB**, sisakan ±11 jam penyangga.
- Hari 1 praktis dimulai malam hari. Rencanakan hari 1 sebagai sesi malam, bukan sehari penuh.
- Akhir pekan (31 Okt–1 Nov) bertepatan dengan **gajian dan belanja bulanan**, saat kebutuhan pokok dibeli sekaligus. Itu jendela terbaik untuk uji lapangan.

## 2. Tiga jalur paralel (kunci agar tidak menganggur)

| Jalur | Isi | Berjalan sendiri? |
|---|---|---|
| **Build** | Kode: ekstraksi, DB, rekomendasi, UI | Butuh kamu 3 jam/hari |
| **Data** | Struk baru, harga toko, ground truth, label rak | Berjalan di sela aktivitas harian (istri belanja = data masuk) |
| **Orang & konten** | Penguji, testimoni, b-roll video, naskah | Dikerjakan paralel, tidak menunggu kode |

Satu hari 4 jam: **3 jam Build, 30 menit Data, 30 menit Orang/Konten + catatan harian**. Kalau Build macet, pindah ke daftar di bagian 6; jangan diam.

## 3. Hitung mundur 22–26 Okt

### Kamis 22 Okt (hari ini) [AMAN]
- [ ] Buat akun dan kunci di dev.meta.ai, lalu panggil `GET https://api.meta.ai/v1/models` (cek akses dari Kudus dan model apa saja yang muncul). Catat kredit yang sebenarnya.
- [ ] **Pasang metode pembayaran** (kartu yang bisa transaksi internasional) dan **atur spend limit alert**. Tanpa metode pembayaran, request pertama gagal dengan 402 `billing_not_configured` (lihat [META-API.md](META-API.md) bagian 5).
- [ ] Baca [Terms of Service](https://dev.meta.ai/legal/terms-of-service) dan [AUP](https://dev.meta.ai/legal/acceptable-use-policy) sendiri (±20 menit), terutama §1 (penggunaan konsumen), §6 (Contributor), dan §10 (18+, larangan distilasi). Ringkasanku bukan nasihat hukum.
- [ ] Kirim pertanyaan ke panitia (Discord/email di halaman acara): *boleh melakukan uji kelayakan API dan persiapan data sebelum 26 Okt? Apakah akses dari Indonesia dan kredit sudah aktif?* Simpan jawabannya.
- [ ] Minta istri menyimpan **semua struk** mulai sekarang. Minta 5–10 keluarga atau teman mengirim foto struk belanja mereka (tutup nama, nomor member, dan 4 digit kartu).
- [ ] Siapkan folder `data/struk/` dengan penamaan `YYYYMMDD_toko_nn.jpg`.

### Jumat 23 Okt [AMAN]
- [ ] **Eksperimen harga** di [template-harga-15x5.xlsx](template-harga-15x5.xlsx): ke 4–5 toko, catat harga, foto label rak.
- [ ] Putuskan 4–5 toko uji dan beri nama/tipe di template.

### Sabtu 24 Okt [AMAN]
- [ ] Lengkapi eksperimen harga di toko yang tersisa (pasar/warung).
- [ ] **Buat ground truth:** untuk ±30 struk, ketik hasil yang *seharusnya* (toko, tanggal, tiap item + harga + produk kanonik) di spreadsheet. Ini bahan ukur akurasi di hari 1, dan menghemat paling banyak waktu di hari-H. ±3 menit per struk.
- [ ] Susun daftar ±50 produk kanonik (nama, merek, varian, ukuran, satuan, kategori) dari struk dan eksperimen harga.

### Minggu 25 Okt [AMAN]
- [ ] Isi sheet Ringkasan dan baca vonisnya. **Gerbang go/pivot** (bagian 4).
- [ ] Rekrut 2–3 rumah tangga penguji final dan sepakati jadwal uji akhir pekan 31 Okt–1 Nov.
- [ ] Tulis naskah video satu halaman (masalah → foto struk → verify → daftar belanja → rekomendasi → hemat Rp X). Boleh kasar.
- [ ] Putuskan tumpukan teknis di atas kertas (usulan di bawah) dan buat akun Supabase, Vercel, GitHub. **Jangan membuat repo proyek atau menulis kode.**

### Senin 26 Okt (hari-H) 
- [ ] Siang: istirahat, rapikan data, pastikan semua akun bisa masuk, charge perangkat.
- [ ] **20:00 WIB:** baca Official Rules **pertama** (kode sebelum periode, kepemilikan, syarat video/repo/lisensi), lalu tonton kickoff.
- [ ] Setelah rules aman: mulai hari 1 dari rencana di bagian 5. Seharusnya tidak ada keputusan baru yang tersisa.

**Usulan tumpukan teknis (keputusan di atas kertas):** Next.js PWA + Supabase (Postgres, `pg_trgm`) + Vercel. Panggil Meta Model API dari **server** (route handler), jangan dari browser, karena kunci tidak boleh bocor. Kompres foto ke ±1.280 px di klien sebelum dikirim. Pakai SDK OpenAI dengan `base_url=https://api.meta.ai/v1`.

## 4. Gerbang keputusan, Minggu 25 Okt malam

| Hasil eksperimen | Keputusan |
|---|---|
| Hemat bersih 2 toko ≥ 3% dan toko termurah berganti-ganti antar barang | **Go**: wedge "pilih toko" dipertahankan |
| Hemat bersih < 3%, atau satu toko hampir selalu termurah | **Pivot ringan**: tekankan "kapan beli" (promo), pasar/barang segar, atau *pelacak harga pribadi + peringatan naik harga*. Putuskan hari ini, bukan di hari-H. |
| API tidak bisa diakses dari Kudus | Hubungi panitia segera; siapkan rencana B (lihat bagian 7) |

## 5. Kalender 10 hari (WIB)

| Hari | Tanggal | Fokus | Selesai jika |
|---|---|---|---|
| D1 | Sen 26 Okt (malam) | Baca rules, setup proyek, struk → JSON, uji test set | Rules dipahami; skema ekstraksi jalan |
| D2 | Sel 27 | Pemetaan produk kanonik, alias, konversi satuan | Angka akurasi otomatis tercatat |
| D3 | Rab 28 | DB dan layar Review & Verify | 1 struk end-to-end tersimpan |
| D4 | Kam 29 | Daftar belanja (teks) → mesin rekomendasi | Hasil benar pada contoh manual |
| D5 | Jum 30 | Penjelasan rekomendasi, suara, sumber/umur harga. **Gerbang:** alur inti jalan di HP | Siap diuji di dunia nyata |
| D6–D7 | Sab 31 Okt–Min 1 Nov | **Uji lapangan belanja bulanan** + perbaikan di sela | Trip nyata tercatat; angka potensi hemat dan akurasi |
| D8 | Sen 2 Nov | "Masak apa?" tipis + berbagi 2 akun; label rak jika waktu. **Fitur dibekukan** di akhir hari | Alur lengkap, atau fitur dipotong |
| D9 | Sel 3 Nov | Polish, rekam video (+ rekaman cadangan alur inti), README | Video dan README jadi |
| D10 | Rab 4 Nov | Tulisan singkat, kirim **sebelum 22:00 WIB** | Terkirim, dikonfirmasi |

Catatan perubahan dari rencana awal: uji lapangan dimajukan ke akhir pekan karena itu saat belanja bulanan terjadi. Akibatnya alur inti harus siap di **D5**, dan "masak apa?" dipindah ke D8, sesudah uji lapangan. Kalau D8 gagal, fitur itu dipotong, bukan dipaksakan.

## 6. Kalau macet atau menunggu (isi waktu yang berguna)

- Tambah ground truth 10 struk lagi dan jalankan ulang evaluasi.
- Coba varian prompt dan skema, dan catat mana yang menaikkan akurasi.
- Tes aplikasi di HP nyata (instal PWA, kamera, koneksi lambat).
- Rapikan teks antarmuka berbahasa Indonesia.
- Tulis README dan langkah setup, lalu uji di mesin bersih.
- Rekam b-roll: istri belanja, struk, label rak, layar HP.
- Tanya di Discord office hours kalau ada masalah API.
- Isi **log metrik** (di bawah).

## 7. Rencana B

| Risiko | Pertanda | Tindakan |
|---|---|---|
| Akses API dari Indonesia gagal | `GET /v1/models` error/menolak | Hubungi panitia segera; jika tetap gagal, tanyakan solusi resmi (jangan mengakali pembatasan wilayah) |
| Akurasi ekstraksi rendah | < 60% otomatis di akhir D2 | Perkuat UI verify, persempit katalog, eskalasi ke model lebih kuat; jangan tambah fitur |
| Penguji tidak mau memotret | Belum ada struk baru sampai D5 | Pakai struk yang sudah dikumpulkan dan catat sebagai temuan jujur |
| Waktu habis | Gerbang D5 atau D8 meleset | Potong dalam urutan: berbagi 2 akun → "masak apa?" → suara. Pertahankan struk → verify → rekomendasi |
| Demo live gagal | Jaringan/API bermasalah | Rekaman cadangan alur inti sudah ada dari D9 |

## 8. Log harian (isi 5 menit tiap malam)

Bahan angka untuk video dan tulisan. Salin tabel ini ke catatan harian.

| Tanggal | Struk diproses | % otomatis benar | % dikoreksi | Waktu verify/struk | Biaya API (Rp) | Potensi hemat | Catatan/hambatan besok |
|---|---|---|---|---|---|---|---|
| | | | | | | | |
