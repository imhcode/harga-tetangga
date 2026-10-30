package com.imhcode.hargatetangga.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.imhcode.hargatetangga.data.HouseholdRepository
import com.imhcode.hargatetangga.data.HouseholdState
import kotlinx.coroutines.launch

// F5-01/02: create a household, share its code, approve who joins (H7).
@Composable
fun HouseholdSection() {
    var state by remember { mutableStateOf<HouseholdState?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var code by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reload) {
        state = runCatching { HouseholdRepository.load() }.onFailure { error = it.message }.getOrNull()
    }
    fun act(block: suspend () -> Unit) {
        busy = true
        error = null
        scope.launch {
            try {
                block()
                reload++
            } catch (e: Exception) {
                // Database functions raise readable Indonesian messages; show the first line.
                error = e.message?.lineSequence()?.firstOrNull { it.isNotBlank() } ?: "Gagal"
            } finally {
                busy = false
            }
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Rumah tangga", style = MaterialTheme.typography.titleMedium)
            val s = state
            val membership = s?.myMembership
            when {
                s == null -> Text("Memuat…", style = MaterialTheme.typography.bodySmall)

                membership == null -> {
                    Text(
                        "Gabungkan akun keluarga supaya harga, daftar belanja, dan hasil belanja dipakai bersama. Struk tetap pribadi.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = { act { HouseholdRepository.create("Rumah") } }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Buat rumah tangga")
                    }
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase().take(6) },
                        label = { Text("Kode undangan") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        onClick = { act { HouseholdRepository.requestJoin(code) } },
                        enabled = !busy && code.length == 6,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Minta gabung") }
                }

                membership.status == "pending" -> {
                    Text("Menunggu persetujuan pemilik rumah tangga.", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { act { HouseholdRepository.leave() } }, enabled = !busy) { Text("Batalkan permintaan") }
                }

                else -> {
                    if (s.isOwner && s.household != null) {
                        Text("Kode undangan", style = MaterialTheme.typography.labelLarge)
                        Surface(color = MaterialTheme.colorScheme.secondary, shape = MaterialTheme.shapes.small) {
                            Text(
                                s.household.inviteCode,
                                style = MaterialTheme.typography.headlineMedium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                        Text(
                            "Anggota memasukkan kode ini di tab Akun HP-nya, lalu kamu menyetujuinya di sini.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text("Anggota", style = MaterialTheme.typography.labelLarge)
                    s.members.forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(m.email ?: "(tanpa email)", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    when {
                                        m.role == "owner" -> "pemilik"
                                        m.status == "pending" -> "menunggu persetujuan"
                                        else -> "anggota"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (s.isOwner && m.status == "pending") {
                                Button(onClick = { act { HouseholdRepository.decide(m.userId, true) } }, enabled = !busy) { Text("Setujui") }
                                TextButton(onClick = { act { HouseholdRepository.decide(m.userId, false) } }, enabled = !busy) { Text("Tolak") }
                            }
                        }
                    }
                    if (!s.isOwner) {
                        TextButton(onClick = { act { HouseholdRepository.leave() } }, enabled = !busy) { Text("Keluar dari rumah tangga") }
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            TextButton(onClick = { reload++ }) { Text("Muat ulang") }
        }
    }
}
