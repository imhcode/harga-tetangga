package com.imhcode.hargatetangga.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.imhcode.hargatetangga.data.ReportRepository
import com.imhcode.hargatetangga.data.ReportRow
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private data class ProductSpend(val productId: String, val name: String, val important: Boolean, val times: Int, val total: Double)

// F5-03: where the month's grocery money went, by category and by product; F5-04: star what matters.
@Composable
fun ReportScreen() {
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var importantOnly by rememberSaveable { mutableStateOf(false) }
    var rows by remember { mutableStateOf<List<ReportRow>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val ym = YearMonth.parse(month)

    LaunchedEffect(month) {
        rows = null
        error = null
        rows = runCatching { ReportRepository.month(ym) }.onFailure { error = "Laporan gagal dimuat: ${it.message}" }.getOrNull()
    }

    val shown = rows.orEmpty().filter { !importantOnly || it.products.important }
    val total = shown.sumOf { it.spend }
    val byCategory = shown.groupBy { it.products.category ?: "Lainnya" }
        .mapValues { (_, r) -> r.sumOf { it.spend } }
        .toList().sortedByDescending { it.second }
    val byProduct = shown.groupBy { it.productId }.map { (id, r) ->
        ProductSpend(id, r.first().products.name, r.first().products.important, r.size, r.sumOf { it.spend })
    }.sortedByDescending { it.total }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Laporan", style = MaterialTheme.typography.headlineMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { month = ym.minusMonths(1).toString() }) { Text("‹") }
                Text(
                    "${ym.month.getDisplayName(TextStyle.FULL, Locale("id", "ID"))} ${ym.year}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { month = ym.plusMonths(1).toString() }, enabled = ym < YearMonth.now()) { Text("›") }
            }
            FilterChip(selected = importantOnly, onClick = { importantOnly = !importantOnly }, label = { Text("Penting saja") })
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (rows == null && error == null) item { CircularProgressIndicator() }

        if (rows != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Belanja tercatat", style = MaterialTheme.typography.labelLarge)
                        Text(rupiah(total), style = MaterialTheme.typography.displaySmall)
                        Text(
                            "${shown.size} baris dari struk yang sudah diperiksa" + if (importantOnly) ", hanya barang penting." else ".",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            if (shown.isEmpty()) {
                item {
                    Text(
                        if (importantOnly) "Belum ada barang bertanda penting bulan ini. Ketuk bintang di daftar barang."
                        else "Belum ada struk bulan ini.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (byCategory.isNotEmpty()) {
                item { Text("Per kategori", style = MaterialTheme.typography.titleMedium) }
                items(byCategory, key = { it.first }) { (category, spend) ->
                    val share = if (total > 0) (spend / total).toFloat() else 0f
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row {
                            Text(category, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text("${rupiah(spend)} · ${(share * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                        }
                        Box(Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)) {
                            Box(Modifier.fillMaxWidth(share.coerceAtLeast(0.01f)).height(8.dp).background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small))
                        }
                    }
                }
            }
            if (byProduct.isNotEmpty()) {
                item {
                    Text("Per barang", style = MaterialTheme.typography.titleMedium)
                    Text("Bintang = penting. Dipakai untuk filter di atas, dan berlaku untuk seluruh rumah tangga.", style = MaterialTheme.typography.bodySmall)
                }
                items(byProduct, key = { it.productId }) { p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            scope.launch {
                                runCatching { ReportRepository.setImportant(p.productId, !p.important) }
                                    .onSuccess { rows = ReportRepository.month(ym) }
                                    .onFailure { error = "Gagal menandai: ${it.message}" }
                            }
                        }) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = if (p.important) "Hapus tanda penting" else "Tandai penting",
                                tint = if (p.important) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.bodyMedium)
                            Text("${p.times}× dibeli", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(rupiah(p.total), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
