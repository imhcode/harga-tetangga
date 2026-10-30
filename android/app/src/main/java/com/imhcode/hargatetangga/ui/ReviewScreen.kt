package com.imhcode.hargatetangga.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.imhcode.hargatetangga.data.CatalogProduct
import com.imhcode.hargatetangga.data.ReceiptDraft

@Composable
fun ReviewScreen(vm: ReceiptViewModel, draft: ReceiptDraft) {
    val toCheck = vm.edits.filter { !it.line.autoAccept }
    val accepted = vm.edits.filter { it.line.autoAccept }
    val discounts = draft.lines.filter { it.kind == "discount" }
    val keptCount = vm.edits.count { !it.skip }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Periksa hasil", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Diekstraksi AI, periksa dulu. Dibaca dalam ${"%.1f".format(draft.extractMs / 1000.0)} detik" +
                            if (draft.escalated) " (dibaca ulang lebih teliti)." else ".",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            item { StoreCard(vm, draft) }
            item { TotalCheck(draft) }
            if (draft.unreadable.isNotEmpty()) {
                item {
                    Text(
                        "Tidak terbaca: " + draft.unreadable.joinToString("; "),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (toCheck.isNotEmpty()) {
                item { SectionTitle("Perlu dicek (${toCheck.size})") }
                items(toCheck, key = { it.line.index }) { LineCard(it, draft.catalog) }
            }
            if (accepted.isNotEmpty()) {
                item { SectionTitle("Diterima otomatis (${accepted.size})") }
                items(accepted, key = { it.line.index }) { LineCard(it, draft.catalog) }
            }
            if (discounts.isNotEmpty()) {
                item { SectionTitle("Diskon dan voucher") }
                item {
                    Text(
                        "Tidak disimpan sebagai harga. Harga barang dicatat sesuai harga rak.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                items(discounts, key = { "d" + it.index }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(it.rawName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(rupiah(it.lineTotal), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Surface(tonalElevation = 3.dp) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (!vm.canSave) {
                    Text(
                        "Isi nama toko, nama produk, dan harga satuan di setiap baris yang disimpan.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(
                    onClick = vm::save,
                    enabled = vm.canSave && !vm.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (vm.saving) "Menyimpan…" else "Simpan $keptCount harga") }
                TextButton(onClick = vm::reset, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Batal") }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun StoreCard(vm: ReceiptViewModel, draft: ReceiptDraft) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Toko", style = MaterialTheme.typography.titleMedium)
            if (draft.stores.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(draft.stores, key = { it.id ?: it.name }) { s ->
                        FilterChip(
                            selected = vm.store.id != null && vm.store.id == s.id,
                            onClick = { vm.chooseStore(s) },
                            label = { Text(listOfNotNull(s.name, s.branch).joinToString(" · ")) },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = vm.store.name,
                onValueChange = { vm.editStore(name = it) },
                label = { Text("Nama toko") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = vm.store.branch ?: "",
                onValueChange = { vm.editStore(branch = it) },
                label = { Text("Cabang (boleh kosong)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(STORE_KINDS, key = { it.first }) { (value, label) ->
                    FilterChip(
                        selected = vm.store.kind == value,
                        onClick = { vm.editStore(kind = value) },
                        label = { Text(label) },
                    )
                }
            }
            OutlinedTextField(
                value = vm.purchaseDate,
                onValueChange = { vm.purchaseDate = it },
                label = { Text("Tanggal belanja (TTTT-BB-HH)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TotalCheck(draft: ReceiptDraft) {
    val color = if (draft.sumMatches) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            if (draft.sumMatches) "Jumlah semua baris cocok dengan total struk" else "Jumlah baris tidak cocok dengan total struk",
            color = color,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            "Jumlah baris ${rupiah(draft.lineSum)} · Total struk ${rupiah(draft.printedTotal)}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LineCard(edit: LineEdit, catalog: List<CatalogProduct>) {
    val line = edit.line
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { edit.open = !edit.open },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(edit.name, style = MaterialTheme.typography.titleSmall)
                    Text(line.rawName, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    Text(
                        "${quantityText(parseQuantity(edit.quantity))} × ${rupiah(parsePrice(edit.unitPrice))} = ${rupiah(parsePrice(edit.lineTotal))}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    when {
                        edit.skip -> "Dilewati"
                        edit.corrected -> "Diubah"
                        line.autoAccept -> "OK"
                        else -> "Cek"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (edit.skip || line.autoAccept || edit.corrected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                )
            }
            if (line.flags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    line.flags.forEach { flag ->
                        // Shelf-label yellow tag, the app's marker for "look at this".
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.padding(vertical = 2.dp),
                        ) {
                            Text(
                                FLAG_LABELS[flag] ?: flag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
            if (edit.open) {
                HorizontalDivider()
                OutlinedTextField(
                    value = edit.name,
                    onValueChange = { edit.rename(it, catalog) },
                    label = { Text("Produk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    if (edit.productId != null) "Cocok dengan produk yang sudah tersimpan." else "Akan disimpan sebagai produk baru.",
                    style = MaterialTheme.typography.bodySmall,
                )
                val typed = edit.name.trim()
                val suggestions = catalog.filter {
                    it.id != edit.productId && typed.length >= 3 &&
                        typed.split(" ").filter { w -> w.length >= 3 }.any { w -> it.name.contains(w, ignoreCase = true) }
                }.take(3)
                if (suggestions.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        suggestions.forEach { p -> AssistChip(onClick = { edit.choose(p) }, label = { Text(p.name) }) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Jumlah", edit.quantity, { edit.quantity = it }, Modifier.weight(0.7f))
                    NumberField("Harga satuan", edit.unitPrice, { edit.unitPrice = it }, Modifier.weight(1f))
                    NumberField("Total", edit.lineTotal, { edit.lineTotal = it }, Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = edit.skip, onCheckedChange = { edit.skip = it })
                    Text("Jangan simpan baris ini", style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = { edit.open = false }) { Text("Tutup") }
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}
