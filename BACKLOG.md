# Backlog HargaTetangga v1.0

Dibuat 30 Okt 2026 dari [PRD.md](PRD.md). Backlog ini menggantikan jadwal di PRD bagian 12.

## Kondisi awal

- Belum ada kode. Folder proyek hanya berisi dokumen, dan repo GitHub `imhcode/harga-tetangga` masih kosong.
- Waktu tersisa: Jum 30 Okt sampai Rab 4 Nov, 6 hari × ±4 jam = **±24 jam**. Submit sebelum Rab 4 Nov 22:00 WIB.
- Total estimasi semua fase: **±29 jam**. Artinya tidak semua fase muat. Garis potong ada di bagian "Garis potong".

## Cara kerja

**E2E per fase.** Setiap fase menghasilkan irisan vertikal yang jalan di HP fisik:
aplikasi Android → Supabase Edge Function → Meta Model API → Postgres → kembali ke layar.
Tidak ada fase yang hanya "backend saja" atau "UI saja". Setelah setiap fase, aplikasi bisa didemokan apa adanya.

**HITL dalam pengembangan.** Ada tiga peran:

| Peran | Tugas |
|---|---|
| Kamu | Memutuskan, menjalankan di HP, meninjau diff, menyetujui commit, mengisi ground truth |
| Claude | Menulis kode, migrasi, script uji, dan dokumen per item |
| Istri | Pengguna nyata: belanja, memotret struk, memakai estimasi |

Aturannya:
1. Claude mengerjakan satu item, kamu meninjau diff dan mencobanya. Commit hanya setelah kamu setuju.
2. Setiap fase ditutup dengan **gerbang HITL**: daftar cek yang kamu jalankan sendiri di HP. Fase berikutnya tidak dimulai sebelum gerbang lulus atau kamu memutuskan memotong.
3. Keputusan potong, pivot, dan pembekuan fitur selalu dari kamu, dicatat di bagian "Log keputusan".

**HITL dalam produk.** Berlaku di semua fase. AI tidak pernah menulis data tanpa konfirmasi manusia:

| # | Titik | Aturan |
|---|---|---|
| H1 | Kirim foto | Foto dikirim ke AI hanya setelah pengguna melihat pratinjau dan menekan Kirim |
| H2 | Hasil ekstraksi | Tidak tersimpan sebelum layar Review & Verify dikonfirmasi |
| H3 | Alias | Alias baru hanya lahir dari koreksi atau konfirmasi manusia |
| H4 | Daftar belanja | Hasil parse teks, suara, atau menu harus dicentang sebelum masuk daftar |
| H5 | Agen | Agen hanya membaca dan menghitung. Tindakan menulis (menambah ke daftar) tampil sebagai usulan yang ditekan pengguna |
| H6 | Kaskade | Hasil dari Wonder tetap berakhir di layar Review, bukan langsung tersimpan |
| H7 | Keluarga | Anggota baru masuk hanya setelah pemilik menekan Setuju |

## Ringkasan fase

| Fase | Hasil e2e di akhir fase | Prioritas | Jam | Target selesai |
|---|---|---|---|---|
| F0 | Tombol di HP memanggil Spark lewat server dan menampilkan jawabannya | Fondasi | 3 | Jum 30 Okt |
| F1 | Foto struk di HP → Review → harga tersimpan dan tampil per toko | P0 | 6,7 | Sab 31 Okt |
| F2 | Daftar belanja → estimasi uang + toko termurah + agen yang menjelaskan | P0 | 5,5 | Min 1 Nov |
| F3 | Satu trip nyata tercatat penuh: estimasi sebelum, struk sesudah, metrik terhitung | P0 | 3 | Sen 2 Nov |
| F4 | Kedalaman AI: kaskade, "mau masak X", suara, tanya riwayat, privasi | P1 | 4,5 | Sen 2 Nov (sisa jam) |
| F5 | Keluarga dan laporan bulanan | P1 | 3 | Dipotong bila jam habis |
| F6 | APK rilis, video, README, submit | Wajib | 3 | Sel 3 – Rab 4 Nov |

Kolom "Siapa" di tabel item: **C** Claude, **K** kamu, **I** istri.

---

## F0 Fondasi dan bukti akses

**Tujuan:** membuktikan semua pipa tersambung sebelum membangun fitur. Risiko #1 (akses Meta API dari Indonesia) dijawab di sini.

**Demo e2e di akhir fase:** tekan tombol "Tes AI" di HP → Edge Function → `muse-spark-1.3` → teks jawaban tampil di layar.

| ID | Item | Selesai kalau | Siapa | Jam |
|---|---|---|---|---|
| F0-01 | Baca Official Rules hackathon | Aturan soal kode, video, repo, dan lisensi dicatat di Log keputusan | K | 0,3 |
| F0-02 | Akun Meta Model API, metode pembayaran, spend alert, kunci API | `GET /v1/models` dari Kudus mengembalikan `muse-spark-1.3` | K | 0,3 |
| F0-03 | Uji vision dengan curl atau script pada 3 struk nyata, memakai JSON schema strict | Tiga JSON valid keluar; catatan akurasi kasar | C+K | 0,5 |
| F0-04 | Repo git lokal, `.gitignore` untuk kunci dan `local.properties`, push ke GitHub | Repo terisi; tidak ada rahasia di riwayat | C+K | 0,2 |
| F0-05 | Proyek Supabase, secret `MODEL_API_KEY`, Edge Function `ping-model` | `curl` ke function mengembalikan jawaban Spark | C+K | 0,5 |
| F0-06 | Proyek Android Kotlin + Compose, `supabase-kt`, login email satu akun | Aplikasi terpasang di HP dan bisa login | C | 0,8 |
| F0-07 | Tombol "Tes AI" memanggil `ping-model` | Jawaban tampil di HP fisik | C | 0,4 |

**Gerbang HITL F0**
- [ ] Jawaban Spark tampil di HP fisik, bukan emulator. Di MuMu sudah jalan (30 Okt: "Beras adalah kebutuhan pokok dapur.", muse-spark-1.3, 1.828 ms); HP fisik belum
- [x] Cari string `LLM_` di APK dan repo: tidak ditemukan (30 Okt)
- [ ] Official Rules tidak melarang pendekatan ini. Ringkasan aturan ada di IDEA.md (kode baru selama acara, repo publik tanpa rahasia dan struk asli); tunggu konfirmasimu
- [ ] F0-03 menunjukkan struk Indonesia bisa dibaca. Script siap (`tools/receipt-probe.ts`), belum dijalankan pada struk nyata

**Status F0 (30 Okt):** F0-02, F0-04 sampai F0-07 selesai. Supabase project `oytzkvclenotyjvqwpkb` (ap-southeast-2), `ping-model` ter-deploy dan menolak anon key, sehingga hanya user yang login bisa memakai kredit Meta.

**Jika gagal:** akses API ditolak → hubungi panitia hari itu juga, dan lanjutkan F1 dengan Edge Function yang mengembalikan JSON contoh dari F0-03 agar UI tetap maju. Android tidak berjalan dalam 1,5 jam → putuskan di Log keputusan apakah tetap Kotlin.

---

## F1 Struk → harga tersimpan (P0)

**Tujuan:** jalur data utama berjalan dari foto sampai harga yang bisa dipercaya.

**Demo e2e di akhir fase:** foto struk di HP → pratinjau dan Kirim (H1) → ekstraksi + cek konsistensi + pencocokan alias → layar Review & Verify (H2, H3) → harga tersimpan → layar Harga menampilkan barang itu per toko beserta umur harga.

| ID | Item | Selesai kalau | Siapa | Jam |
|---|---|---|---|---|
| F1-01 | Migrasi DB: `stores`, `products`, `product_aliases`, `receipts`, `price_observations`, ekstensi `pg_trgm`, RLS per pengguna | Migrasi jalan bersih di Supabase | C | 0,6 |
| F1-02 | Seed ±50 produk pokok dan 4–5 toko uji | Claude membuat draf, kamu menyetujui daftar dan nama toko | C+K | 0,4 |
| F1-03 | Kamera (CameraX), kompres ±1.280 px, layar pratinjau dengan tombol Kirim | Foto terkirim hanya setelah Kirim ditekan | C | 0,8 |
| F1-04 | Edge Function `extract-receipt`: Chat Completions, gambar inline, JSON schema strict, `reasoning_effort: low`, asal teks per nilai | Struk uji menghasilkan JSON valid; batas token cukup | C | 0,8 |
| F1-05 | Cek konsistensi deterministik: jumlah baris vs total, harga per satuan wajar, nilai tak terbaca | Baris bermasalah diberi tanda dan alasan | C | 0,5 |
| F1-06 | Pencocokan alias: shortlist `pg_trgm`, Spark memilih produk kanonik dan memberi tingkat yakin | Setiap baris membawa kandidat produk dan nilai yakin | C | 0,8 |
| F1-07 | Layar Review & Verify: baris yakin otomatis diterima, baris bertanda terbuka untuk dikoreksi, label "Diekstraksi AI, periksa dulu" | Koreksi menyimpan alias baru | C | 1,0 |
| F1-08 | Simpan `price_observations` dengan sumber, tanggal, status verifikasi | Data muncul di tabel setelah konfirmasi | C | 0,3 |
| F1-09 | Layar Harga: daftar barang, harga per toko, umur harga, "belum ada data" | Barang dari struk tampil benar | C | 0,6 |
| F1-10 | Ground truth: ketik baris yang benar untuk 10 struk | 10 file ground truth tersimpan | K | 0,5 |
| F1-11 | Script ukur akurasi baris dan waktu per struk, dicatat ke `metrics/` | Angka akurasi keluar untuk 10 struk | C | 0,4 |

**Gerbang HITL F1**
- [ ] Tiga struk nyata dari dua toko berbeda selesai e2e di HP
- [ ] Koreksi satu nama barang, lalu foto struk lain dengan barang yang sama: alias terpakai otomatis
- [ ] Akurasi baris pada 10 struk tercatat. Ambang: ≥85% target, <60% pivot
- [ ] Waktu dari foto sampai tersimpan dicatat

**Jika gagal:** akurasi <60% → perkuat layar Review dan persempit katalog, F4 dipotong lebih dulu. Latensi >30 detik → coba `stream: true` atau turunkan reasoning.

---

## F2 Daftar belanja → estimasi uang dan toko termurah (P0, fitur andalan)

**Tujuan:** menjawab cerita pemicu: berapa uang yang dibawa dan belanja di mana.

**Demo e2e di akhir fase:** tulis daftar belanja bebas → Spark mengurai jadi item → centang (H4) → layar Estimasi menampilkan total, rentang, "bawa Rp X", toko termurah, dan kombinasi dua toko → tanya agen "kenapa toko B?" → jawaban menyebut sumber dan umur harga.

| ID | Item | Selesai kalau | Siapa | Jam |
|---|---|---|---|---|
| F2-01 | Migrasi `shopping_lists`, `shopping_list_items`, `estimates` (cuplikan estimasi sebelum berangkat) | Migrasi jalan | C | 0,3 |
| F2-02 | Edge Function `parse-list`: teks bebas → item terstruktur → dipetakan ke produk | "beras 5kg, minyak 2 liter, telur sekilo" terurai benar | C | 0,6 |
| F2-03 | Layar daftar belanja dengan centang konfirmasi | Hanya item yang dicentang masuk daftar | C | 0,6 |
| F2-04 | Mesin estimasi deterministik: total, rentang dari umur dan variasi harga, "bawa Rp X" dengan cadangan yang bisa diatur | Hasil cocok dengan hitungan manual pada 1 contoh | C | 0,7 |
| F2-05 | Mesin rekomendasi: kombinasi 1 dan 2 toko + biaya tetap per toko tambahan; barang tanpa data ditandai | Hasil cocok dengan hitungan manual | C | 0,6 |
| F2-06 | Edge Function `agent`: loop tool calling dengan `cari_harga`, `hitung_estimasi`, `bandingkan_toko`; argumen divalidasi di server | Agen menjawab dengan angka dari tool, bukan karangan | C | 1,2 |
| F2-07 | Layar Estimasi dan panel tanya agen; usulan agen tampil sebagai tombol (H5) | Agen tidak bisa mengubah daftar tanpa tombol ditekan | C | 0,9 |
| F2-08 | Tombol "Berangkat belanja" menyimpan cuplikan estimasi | Cuplikan tersimpan dengan waktu | C | 0,2 |
| F2-09 | Uji 10 pertanyaan agen yang kamu tulis | Kamu menilai setiap jawaban: benar, salah, atau mengarang | K | 0,4 |

**Gerbang HITL F2**
- [ ] Istri membuat daftar belanja nyata sendiri tanpa dibantu
- [ ] Estimasi dan rekomendasi dicek manual pada satu daftar: angkanya cocok
- [ ] Barang tanpa data tidak ditebak harganya
- [ ] Dari 10 pertanyaan F2-09, tidak ada jawaban yang mengarang angka

**Jika gagal:** agen tidak stabil → layar Estimasi tetap jalan tanpa agen (mesin deterministik sudah cukup untuk demo), dan agen diperbaiki di F4.

---

## F3 Uji lapangan dan metrik (P0)

**Tujuan:** membuktikan dampak dengan belanja nyata. Fase ini sebagian besar HITL.

**Demo e2e di akhir fase:** daftar → estimasi → berangkat → belanja → foto struk → Review → layar Hasil Trip: estimasi vs total kasir, item di luar rencana, waktu verifikasi.

| ID | Item | Selesai kalau | Siapa | Jam |
|---|---|---|---|---|
| F3-01 | Perbandingan rencana vs struk: item di luar rencana ditandai netral (PRD 6.9) | Item struk yang tidak ada di daftar ditandai | C | 0,5 |
| F3-02 | Layar Hasil Trip: selisih estimasi vs total, item di luar rencana | Angka tampil untuk satu trip | C | 0,5 |
| F3-03 | Kumpulan metrik di `metrics/`: akurasi baris, selisih estimasi, % item di luar rencana, waktu verifikasi | Satu perintah menghasilkan tabel metrik | C | 0,4 |
| F3-04 | Trip nyata #1 dan #2 dengan aplikasi | Dua trip tercatat penuh | I | – |
| F3-05 | Wawancara singkat istri setelah trip: waktu, rasa, yang membingungkan | Catatan tertulis 5–10 baris | K+I | 0,3 |
| F3-06 | Triase bug dari trip: perbaiki yang memblokir alur | Tidak ada bug pemblokir | C+K | 1,3 |

**Gerbang HITL F3**
- [ ] Minimal dua trip nyata tercatat penuh
- [ ] Tabel metrik terisi dan kamu percaya angkanya
- [ ] Kamu memutuskan item F4 dan F5 mana yang dikerjakan, dicatat di Log keputusan

**Jika gagal:** istri belum sempat belanja → simulasikan satu trip dengan struk dari koleksi, dan tulis jujur di README bahwa uji lapangan terbatas.

---

## F4 Kedalaman AI (P1)

**Tujuan:** menambah titik pemakaian model yang dinilai juri. Dikerjakan berurutan; berhenti saat jam habis.

| ID | Item | Selesai kalau | Siapa | Jam |
|---|---|---|---|---|
| F4-01 | Kaskade Spark → Wonder untuk baris bertanda; catat biaya dan akurasi per tingkat (H6) | Tabel perbandingan tingkat pada 10 struk ground truth | C | 1,0 |
| F4-02 | "Mau masak X": tool `buat_daftar_dari_menu`, bahan dicentang atau dilewati karena ada di rumah (H4) | Satu menu jadi item daftar setelah dicentang | C | 1,0 |
| F4-03 | Suara: `AudioRecord` WAV 24 kHz mono, tahan untuk bicara, Edge Function `asr` → `parse-list` | Daftar yang diucapkan masuk ke layar centang | C | 1,0 |
| F4-04 | Tanya riwayat: tool `riwayat_pembelian` di agen yang sama | "Kapan terakhir beli minyak?" dijawab dengan tanggal, toko, harga | C | 0,5 |
| F4-05 | Tanya sebelum kirim: tutup area struk (nama, nomor member, kartu) dan log egress | Area yang ditutup tidak ada di gambar terkirim | C | 1,0 |

**Gerbang HITL F4**
- [ ] Setiap item yang selesai dicoba di HP dengan data nyata
- [ ] Item yang tidak selesai dicabut dari menu aplikasi, bukan dibiarkan setengah jadi

---

## F5 Keluarga dan laporan (P1)

| ID | Item | Selesai kalau | Siapa | Jam |
|---|---|---|---|---|
| F5-01 | Migrasi `households`, `household_members`, `household_invites`; RLS berbagi observasi harga | Dua akun dalam satu rumah tangga melihat harga yang sama; struk tetap pribadi | C | 0,8 |
| F5-02 | Buat kode undangan, masukkan kode, pemilik menekan Setuju (H7) | HP kedua bergabung setelah disetujui | C | 1,0 |
| F5-03 | Laporan bulanan: tabel dan grafik per kategori, filter "penting" | Laporan bulan Oktober–November tampil | C | 1,0 |
| F5-04 | Tandai barang atau kategori sebagai penting | Filter bekerja | C | 0,2 |

**Gerbang HITL F5**
- [ ] HP istri bergabung ke rumah tangga lewat kode dan melihat harga dari struk kamu
- [ ] Foto struk milik orang lain tidak bisa dibuka

---

## F6 Rilis dan submit (wajib)

**Fitur dibekukan Sen 2 Nov malam.** Setelah itu hanya perbaikan bug dan materi submit.

| ID | Item | Selesai kalau | Siapa | Jam |
|---|---|---|---|---|
| F6-01 | Onboarding: kebijakan privasi sederhana, batas 18+, label AI | Tampil saat pertama buka | C | 0,4 |
| F6-02 | APK rilis bertanda tangan; pasang bersih di HP lain | Alur demo jalan dari instalasi baru | C+K | 0,4 |
| F6-03 | README berbahasa Inggris: masalah, arsitektur, pemakaian model, metrik, cara menjalankan | README di repo | C+K | 0,6 |
| F6-04 | Naskah video 2–3 menit, dibuka dengan cerita nyata | Kamu menyetujui naskah | C+K | 0,3 |
| F6-05 | Rekam video dan satu cadangan | Kamu menonton ulang dan menyetujui | K | 1,0 |
| F6-06 | Isi formulir submit; simpan bukti terkirim | Konfirmasi diterima sebelum Rab 4 Nov 22:00 WIB | K | 0,3 |

**Gerbang HITL F6**
- [ ] Alur demo lengkap jalan dari instalasi baru di HP yang belum pernah dipakai
- [ ] Angka di video dan README sama dengan tabel metrik
- [ ] Bukti submit tersimpan

---

## Jadwal harian

| Hari | Rencana | Gerbang |
|---|---|---|
| Jum 30 Okt | F0 seluruhnya, mulai F1-01 sampai F1-03 | F0 |
| Sab 31 Okt | F1 selesai; istri mulai mengumpulkan struk baru | F1 |
| Min 1 Nov | F2 | F2 |
| Sen 2 Nov | Trip nyata #1, F3, lalu F4 sesuai sisa jam. **Beku fitur malam ini** | F3, F4 |
| Sel 3 Nov | Trip nyata #2, perbaikan bug, F6-01 sampai F6-05 | – |
| Rab 4 Nov | Submit pagi atau siang, jangan menunggu malam | F6 |

## Garis potong

Dengan ±24 jam: **F0 + F1 + F2 + F3 + F6 = ±21 jam**. Sisa ±3 jam untuk F4, dikerjakan berurutan dari F4-01. **F5 kemungkinan besar tidak muat**; keluarga dan laporan jadi bagian visi di video dan README.

Urutan potong bila waktu lebih sempit (yang atas dipotong lebih dulu):

1. F5 seluruhnya
2. F4-05 Tanya sebelum kirim
3. F4-04 Tanya riwayat
4. F4-03 Suara
5. F4-02 "Mau masak X"
6. F4-01 Kaskade
7. F2-06 dan F2-07 agen (Estimasi tetap jalan tanpa agen)

F0–F3 kecuali agen, dan F6, tidak dipotong.

Catatan: urutan potong F5 berbeda dari PRD bagian 10, yang menaruh akun keluarga di atas "mau masak X". Alasannya, F5 tidak menambah titik pemakaian model, sedangkan F4 menambahnya. Ubah bila kamu tidak setuju.

## Log keputusan

| Tanggal | Keputusan | Alasan |
|---|---|---|
| 27 Okt | Android native Kotlin + Compose, Supabase, PRD berbahasa Indonesia | Sesi tanya-jawab PRD |
| 30 Okt | Backlog e2e per fase dengan gerbang HITL; F5 di bawah garis potong | Belum ada kode, ±24 jam tersisa |
| 30 Okt | Commit langsung ke `main`, push setiap perubahan | Permintaan pemilik; proyek solo |
| 30 Okt | MuMu Player untuk uji harian, HP fisik untuk gerbang | Lebih cepat; gerbang tetap di perangkat nyata |
| 30 Okt | Edge Function wajib memeriksa user login, bukan hanya JWT gateway | Anon key ada di APK dan bisa dipakai siapa saja |
