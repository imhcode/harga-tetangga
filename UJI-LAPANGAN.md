# Panduan Uji Lapangan

Untuk gerbang F1–F3 di [BACKLOG.md](BACKLOG.md). Semua langkah di sini dikerjakan manusia (HITL). Claude membaca hasilnya dari database dan file, lalu memperbaiki yang rusak.

## 1. Persiapan (sekali)

1. **Pasang APK di HP fisik.** File: `android/app/build/outputs/apk/debug/app-debug.apk`. Kirim ke HP (kabel, Google Drive, atau WhatsApp ke diri sendiri), lalu izinkan "instal dari sumber ini".
2. **Gabungkan akun istri ke rumah tangga.** Istri mendaftar akunnya sendiri, lalu di tab Akun memasukkan kode undangan dari HP-mu dan menekan **Minta gabung**. Kamu menekan **Setujui** di tab Akun. Setelah itu harga, daftar belanja, dan hasil belanja dipakai bersama; struk tetap pribadi.
3. **Data uji sudah dihapus** (30 Okt): toko, produk, alias, struk, harga, daftar, estimasi, dan trip uji. Akun login tetap. Semua data mulai sekarang berasal dari belanja nyata.

## 2. Satu trip belanja (F3-04)

| Langkah | Di mana | Siapa |
|---|---|---|
| Tulis daftar belanja, tekan **Urai daftar**, centang, **Tambahkan** | Tab Belanja | Istri |
| Lihat **Bawa uang**. Boleh tanya asisten | Tab Belanja | Istri |
| Tekan **Berangkat belanja** sebelum keluar rumah | Tab Belanja | Istri |
| Belanja seperti biasa. Catat jam berangkat dan pulang | – | Istri |
| Di rumah: **Foto struk**, **Kirim ke AI**, periksa, **Simpan** | Tab Struk | Istri atau kamu |
| **Bandingkan dengan rencana**, koreksi tanda "di luar rencana", **Simpan hasil** | Tab Struk | Istri |

Kalau aplikasi tertutup sebelum membandingkan, buka tab Struk: kartu "Struk dari belanja tadi belum dibandingkan" muncul sendiri.

Targetnya minimal **dua trip penuh** sebelum Sen 2 Nov malam.

## 3. Wawancara singkat setelah trip (F3-05)

Tulis jawabannya di bawah, 1–2 kalimat per pertanyaan.

| # | Pertanyaan | Trip 1 | Trip 2 |
|---|---|---|---|
| 1 | Berapa menit dari berangkat sampai pulang? Lebih cepat atau lambat dari biasa? | | |
| 2 | Uang yang dibawa cukup? Sisa berapa? | | |
| 3 | Bagian mana yang membingungkan atau lambat? | | |
| 4 | Tanda "di luar rencana" terasa membantu atau menghakimi? | | |
| 5 | Mau dipakai lagi minggu depan? Kenapa? | | |

## 4. Sepuluh pertanyaan untuk asisten (F2-09)

Tanyakan di tab Belanja setelah ada beberapa struk tersimpan. Nilai setiap jawaban: **Benar** (angka sesuai tab Harga), **Salah** (angka keliru), atau **Mengarang** (menyebut harga yang tidak ada di data).

| # | Pertanyaan | Nilai | Catatan |
|---|---|---|---|
| 1 | Bawa uang berapa? | | |
| 2 | Belanja di mana paling murah? | | |
| 3 | Harga [barang yang ada di struk] berapa? | | |
| 4 | Harga [barang yang belum pernah dibeli] berapa? | | |
| 5 | Kalau cadangannya 20%, bawa berapa? | | |
| 6 | Mau masak [menu], beli apa saja? | | |
| 7 | Lebih murah beli [barang] di [toko A] atau [toko B]? | | |
| 8 | Total belanjaan ini berapa tanpa cadangan? | | |
| 9 | Barang apa yang belum ada harganya? | | |
| 10 | Pertanyaan bebas dari istri | | |

Gerbang lulus kalau tidak ada satu pun jawaban **Mengarang**.

## 5. Ground truth 10 struk (F1-10)

1. Taruh foto struk di folder `receipts/` (tidak masuk repo).
2. Jalankan pembacaan:
   ```bash
   node --env-file=.env tools/receipt-probe.ts receipts/*.jpg
   ```
3. Buat file kebenaran dari hasil AI:
   ```bash
   node tools/accuracy.ts --init
   ```
4. **Buka setiap `receipts/truth/*.json`, cocokkan dengan foto, perbaiki angka dan nama yang salah.** Ini langkah yang membuat angka akurasi bermakna.
5. Hitung akurasi:
   ```bash
   node tools/accuracy.ts
   ```

## 6. Membaca hasil

Semua angka dari pemakaian nyata ada di satu query:

```bash
npx supabase db query --linked -f tools/metrics.sql
```

| Kolom | Artinya |
|---|---|
| `numbers_read_correctly_pct` | Baris struk yang angkanya tidak perlu dikoreksi |
| `auto_accepted_pct` | Baris yang langsung diterima tanpa dibuka |
| `avg_read_seconds`, `avg_review_seconds` | Waktu AI membaca, waktu manusia memeriksa |
| `trips_cash_was_enough` | Trip yang uang bawanya cukup |
| `estimate_median_error_pct` | Selisih perkiraan vs bayar untuk barang yang direncanakan dan punya harga |
| `off_plan_items_pct`, `off_plan_spend_pct` | Bagian barang dan uang yang di luar rencana |
