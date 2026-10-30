package com.imhcode.hargatetangga.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imhcode.hargatetangga.data.EstimateDto

@Composable
fun ShoppingScreen(vm: ShoppingViewModel = viewModel()) {
    val shopping = vm.list?.status == "shopping"
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Belanja", style = MaterialTheme.typography.headlineMedium)
            Text(
                if (shopping) "Sedang belanja. Setelah selesai, foto struknya di tab Struk."
                else "Tulis daftar belanja, lalu lihat berapa uang yang perlu dibawa.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        vm.busy?.let { item { Column { Text("$it…", style = MaterialTheme.typography.bodySmall); LinearProgressIndicator(Modifier.fillMaxWidth()) } } }
        vm.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }

        item { EstimateCard(vm, shopping) }
        item { ListCard(vm) }
        item { InputCard(vm) }
        item { AgentCard(vm) }
    }
}

@Composable
private fun EstimateCard(vm: ShoppingViewModel, shopping: Boolean) {
    val e = vm.estimate ?: return
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Bawa uang", style = MaterialTheme.typography.labelLarge)
            Text(rupiah(e.bring), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Perkiraan ${rupiah(e.total)} (rentang ${rupiah(e.low)} – ${rupiah(e.high)}) + cadangan ${e.bufferPct}%",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 10, 20).forEach { pct ->
                    FilterChip(selected = vm.bufferPct == pct, onClick = { vm.setBuffer(pct) }, label = { Text("Cadangan $pct%") })
                }
            }
            PlanDetails(e)
            if (shopping) {
                vm.savedAt?.let { Text("Estimasi disimpan pukul $it.", style = MaterialTheme.typography.bodySmall) }
                OutlinedButton(onClick = vm::finishShopping, modifier = Modifier.fillMaxWidth()) { Text("Selesai belanja, mulai daftar baru") }
            } else {
                Button(onClick = vm::startShopping, enabled = vm.busy == null, modifier = Modifier.fillMaxWidth()) {
                    Text("Berangkat belanja")
                }
            }
        }
    }
}

@Composable
private fun PlanDetails(e: EstimateDto) {
    val plan = e.plan
    if (plan == null) {
        Text("Belum ada harga untuk barang di daftar ini. Foto struk saat membelinya.", style = MaterialTheme.typography.bodyMedium)
        return
    }
    HorizontalDivider()
    Text(
        if (plan.storeNames.size == 1) "Paling hemat di ${plan.storeNames[0]}" else "Paling hemat: ${plan.storeNames.joinToString(" + ")}",
        style = MaterialTheme.typography.titleSmall,
    )
    if (plan.extraStoreCost > 0) {
        Text("Termasuk biaya pindah toko ${rupiah(plan.extraStoreCost)}", style = MaterialTheme.typography.bodySmall)
    }
    e.savings?.let {
        Text("Hemat ${rupiah(it.amount)} dibanding belanja semua di ${it.versusStore}", style = MaterialTheme.typography.bodySmall)
    }
    plan.items.forEach { q ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("${q.label} × ${quantityText(q.quantity)}", style = MaterialTheme.typography.bodyMedium)
                Text("${q.storeName} · ${daysAgo(q.observedOn)}", style = MaterialTheme.typography.bodySmall)
            }
            Text(rupiah(q.unitPrice * q.quantity), style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (plan.missing.isNotEmpty()) {
        Surface(color = MaterialTheme.colorScheme.secondary, shape = MaterialTheme.shapes.small) {
            Text(
                "Belum ada data harga, tidak dihitung: " + plan.missing.joinToString(", ") { it.label },
                color = MaterialTheme.colorScheme.onSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}

@Composable
private fun ListCard(vm: ShoppingViewModel) {
    if (vm.items.isEmpty()) return
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Daftar (${vm.items.size})", style = MaterialTheme.typography.titleMedium)
            vm.items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${item.label} × ${quantityText(item.quantity)}", style = MaterialTheme.typography.bodyMedium)
                        if (item.productId == null) Text("belum ada data harga", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { vm.removeItem(item) }) { Text("Hapus") }
                }
            }
        }
    }
}

@Composable
private fun InputCard(vm: ShoppingViewModel) {
    val context = LocalContext.current
    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.startVoice()
    }
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tambah barang", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = vm.draftText,
                onValueChange = { vm.draftText = it },
                placeholder = { Text("beras 5 kg, minyak goreng 2 liter, telur sekilo") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = vm::parse,
                    enabled = vm.busy == null && !vm.recording && vm.draftText.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Text("Urai daftar") }
                // F4-03: tap to start, tap again to send. Recording stops by itself after a minute.
                OutlinedButton(
                    onClick = {
                        when {
                            vm.recording -> vm.stopVoice()
                            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                                PackageManager.PERMISSION_GRANTED -> vm.startVoice()
                            else -> askMic.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    enabled = vm.busy == null,
                    modifier = Modifier.weight(1f),
                ) { Text(if (vm.recording) "Selesai (0:${"%02d".format(vm.recordedSeconds)})" else "Bicara") }
            }
            if (vm.recording) {
                Text("Sebutkan barangnya, misalnya \"beras lima kilo, minyak dua liter\".", style = MaterialTheme.typography.bodySmall)
            }
            vm.transcript?.let {
                Text(if (it.isBlank()) "Tidak ada suara yang terdengar." else "Terdengar: \"$it\"", style = MaterialTheme.typography.bodySmall)
            }
            if (vm.parsed.isNotEmpty()) {
                Text("Centang yang mau dimasukkan", style = MaterialTheme.typography.titleSmall)
                CandidateList(vm.parsed)
                Button(
                    onClick = vm::addParsed,
                    enabled = vm.busy == null && vm.parsed.any { it.checked },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Tambahkan ${vm.parsed.count { it.checked }} barang") }
                TextButton(onClick = vm::cancelParsed) { Text("Batal") }
            }
        }
    }
}

@Composable
private fun CandidateList(candidates: List<Candidate>) {
    candidates.forEach { c ->
        // The whole row toggles, not just the small checkbox.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { c.checked = !c.checked }) {
            Checkbox(checked = c.checked, onCheckedChange = { c.checked = it })
            Column(Modifier.weight(1f)) {
                Text("${c.label} × ${quantityText(c.quantity)}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    c.productName?.let { "cocok: $it" } ?: "belum ada data harga",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgentCard(vm: ShoppingViewModel) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tanya asisten", style = MaterialTheme.typography.titleMedium)
            Text(
                "Asisten hanya memakai harga dari struk yang sudah kamu periksa. Ia bisa mengusulkan barang, kamu yang memutuskan.",
                style = MaterialTheme.typography.bodySmall,
            )
            vm.chat.forEach { m ->
                val mine = m.role == "user"
                Surface(
                    color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(if (mine) 0.85f else 1f).align(if (mine) Alignment.End else Alignment.Start),
                ) {
                    Text(
                        m.content,
                        color = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
            if (vm.proposals.isNotEmpty()) {
                Text("Usulan asisten", style = MaterialTheme.typography.titleSmall)
                Text("Hapus centang barang yang sudah ada di rumah.", style = MaterialTheme.typography.bodySmall)
                CandidateList(vm.proposals)
                Button(
                    onClick = vm::acceptProposals,
                    enabled = vm.busy == null && vm.proposals.any { it.checked },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Tambahkan ${vm.proposals.count { it.checked }} ke daftar") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Bawa uang berapa?", "Belanja di mana paling murah?", "Mau masak sayur sop, beli apa saja?").forEach { q ->
                    AssistChip(onClick = { vm.ask(q) }, enabled = vm.busy == null, label = { Text(q) })
                }
            }
            OutlinedTextField(
                value = vm.question,
                onValueChange = { vm.question = it },
                placeholder = { Text("Tanya soal harga atau menu") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { vm.ask() }, enabled = vm.busy == null && vm.question.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("Kirim")
            }
        }
    }
}
