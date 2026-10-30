package com.imhcode.hargatetangga.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.imhcode.hargatetangga.data.TripPreview
import kotlin.math.abs
import kotlin.math.roundToInt

// F3-02: what was planned vs what the receipt says, before the trip is saved.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TripReviewScreen(vm: ReceiptViewModel, trip: TripPreview) {
    val summary = summarizeTrip(trip, vm.tripLines)
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Hasil belanja", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Periksa tanda \"di luar rencana\". Ini catatan untukmu, bukan penilaian.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CashLine(trip.bring, trip.receiptTotal)
                        TripNumbers(summary)
                    }
                }
            }
            item { Text("Barang di struk", style = MaterialTheme.typography.titleMedium) }
            items(vm.tripLines, key = { it.line.observationId }) { edit ->
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { edit.offPlan = !edit.offPlan },
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(edit.line.productName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (edit.offPlan) "di luar rencana"
                                else edit.listItemLabel?.let { "sesuai daftar: $it" } ?: "direncanakan, tanpa barang daftar",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(rupiah(edit.line.lineTotal), style = MaterialTheme.typography.bodyMedium)
                        Checkbox(checked = edit.offPlan, onCheckedChange = { edit.offPlan = it })
                    }
                    // Automatic match missed? Let the person say which list item this was.
                    val linked = vm.tripLines.mapNotNull { if (it.offPlan) null else it.listItemId }.toSet()
                    val open = trip.items.filter { it.id !in linked }
                    if (edit.offPlan && open.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            open.take(4).forEach { item ->
                                AssistChip(onClick = { edit.linkTo(item.id, item.label) }, label = { Text("ini ${item.label}") })
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "Centang = di luar rencana.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (summary.notBought.isNotEmpty()) {
                item {
                    Text(
                        "Ada di daftar tapi tidak di struk: " + summary.notBought.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        Surface(tonalElevation = 3.dp) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = vm::saveTrip, enabled = !vm.saving, modifier = Modifier.fillMaxWidth()) {
                    Text(if (vm.saving) "Menyimpan…" else "Simpan hasil dan tutup daftar")
                }
                TextButton(onClick = vm::reset, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Nanti saja") }
            }
        }
    }
}

@Composable
fun CashLine(bring: Double?, paid: Double) {
    if (bring == null) {
        Text("Dibayar ${rupiah(paid)}", style = MaterialTheme.typography.titleLarge)
        return
    }
    val left = bring - paid
    Text("Bawa ${rupiah(bring)} · Dibayar ${rupiah(paid)}", style = MaterialTheme.typography.titleMedium)
    Text(
        if (left >= 0) "Uang cukup, sisa ${rupiah(left)}" else "Uang kurang ${rupiah(-left)}",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = if (left >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
    )
}

@Composable
fun TripNumbers(summary: TripSummary) {
    if (summary.plannedEstimated > 0) {
        val diff = summary.plannedActual - summary.plannedEstimated
        val pct = if (summary.plannedActual > 0) abs(diff) / summary.plannedActual * 100 else 0.0
        Text(
            "Barang sesuai rencana: perkiraan ${rupiah(summary.plannedEstimated)}, dibayar ${rupiah(summary.plannedActual)} " +
                "(${if (diff >= 0) "lebih" else "kurang"} ${pct.roundToInt()}%)",
            style = MaterialTheme.typography.bodyMedium,
        )
    } else {
        Text("Belum ada barang dengan perkiraan harga yang bisa dibandingkan.", style = MaterialTheme.typography.bodyMedium)
    }
    Text(
        "Di luar rencana: ${summary.itemsOffPlan} dari ${summary.itemsBought} barang, ${rupiah(summary.offPlanTotal)}",
        style = MaterialTheme.typography.bodyMedium,
    )
}
