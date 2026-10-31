package com.imhcode.hargatetangga.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// F6-01: what the app does with data, the 18+ requirement (Meta ToS §10.1) and the AI label,
// shown once per install before sign-in. Bump the key when the text changes materially.
private const val PREFS = "onboarding"
private const val ACCEPTED_KEY = "accepted_v1"

fun onboardingAccepted(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ACCEPTED_KEY, false)

private fun acceptOnboarding(context: Context) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ACCEPTED_KEY, true).apply()
}

// Shared with the Akun tab so the policy can be reread at any time.
val PRIVACY_POINTS = listOf(
    "Foto struk dikirim ke Meta Model API (mode Standard) untuk dibaca. Meta menyatakan konten mode Standard tidak dipakai untuk melatih modelnya. Lama penyimpanan mengikuti kebijakan Meta.",
    "Sebelum mengirim, kamu bisa menutup nama, nomor member, atau nomor kartu dengan kotak hitam. Bagian itu tidak ikut terkirim.",
    "Foto di HP dihapus setelah struk disimpan. Yang disimpan di server hanya hasil bacaan struk, harga, toko, dan daftar belanja.",
    "Hasil bacaan struk hanya terlihat olehmu. Harga, daftar belanja, dan laporan terlihat oleh anggota rumah tangga yang kamu setujui.",
    "Rekaman suara daftar belanja dikirim untuk ditranskripsi dan tidak disimpan oleh aplikasi.",
    "Tab Akun mencatat setiap foto yang pernah dikirim ke AI.",
)

@Composable
fun OnboardingScreen(context: Context, onAccepted: () -> Unit) {
    var adult by rememberSaveable { mutableStateOf(false) }
    var understood by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("HargaTetangga", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Catat harga dari struk belanja, lalu ketahui berapa uang yang perlu dibawa dan di toko mana paling hemat.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text("Data dan privasi", style = MaterialTheme.typography.titleMedium)
        PRIVACY_POINTS.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        Text("Tentang AI", style = MaterialTheme.typography.titleMedium)
        Text(
            "Struk dibaca dan daftar belanja diurai oleh AI. AI bisa salah, jadi aplikasi selalu memintamu memeriksa sebelum menyimpan. " +
                "Aplikasi ini mencatat dan membandingkan harga; ini bukan nasihat keuangan.",
            style = MaterialTheme.typography.bodyMedium,
        )
        ConsentRow(adult, { adult = it }, "Saya berusia 18 tahun atau lebih.")
        ConsentRow(understood, { understood = it }, "Saya mengerti cara data saya dipakai dan bahwa hasil AI perlu diperiksa.")
        Button(
            onClick = {
                acceptOnboarding(context)
                onAccepted()
            },
            enabled = adult && understood,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Setuju dan lanjut") }
    }
}

@Composable
private fun ConsentRow(checked: Boolean, onChange: (Boolean) -> Unit, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
