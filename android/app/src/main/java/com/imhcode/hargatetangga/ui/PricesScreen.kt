package com.imhcode.hargatetangga.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.imhcode.hargatetangga.data.LatestPrice
import com.imhcode.hargatetangga.data.ReceiptRepository

// F1-09: newest confirmed price of every product at every store.
@Composable
fun PricesScreen() {
    var prices by remember { mutableStateOf<List<LatestPrice>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) {
        error = null
        try {
            prices = ReceiptRepository.latestPrices()
        } catch (e: Exception) {
            error = "Harga gagal dimuat: ${e.message}"
        }
    }

    val grouped = prices.orEmpty().groupBy { it.productId }.values.sortedBy { it.first().productName.lowercase() }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Harga", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { reload++ }) { Text("Muat ulang") }
            }
            Text(
                "Harga terakhir tiap barang di tiap toko, dari struk yang sudah kamu periksa.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        when {
            prices == null && error == null -> item { CircularProgressIndicator() }
            prices?.isEmpty() == true -> item {
                Text(
                    "Belum ada harga. Foto struk pertama di tab Struk, periksa, lalu simpan.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        items(grouped, key = { it.first().productId }) { rows -> ProductPrices(rows) }
    }
}

@Composable
private fun ProductPrices(rows: List<LatestPrice>) {
    val sorted = rows.sortedBy { it.unitPrice }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column {
                Text(sorted.first().productName, style = MaterialTheme.typography.titleMedium)
                sorted.first().category?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            sorted.forEachIndexed { i, p ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(listOfNotNull(p.storeName, p.storeBranch).joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                        Text(daysAgo(p.observedOn), style = MaterialTheme.typography.bodySmall)
                    }
                    if (i == 0 && sorted.size > 1) {
                        Surface(color = MaterialTheme.colorScheme.secondary, shape = MaterialTheme.shapes.extraSmall) {
                            Text(
                                "Termurah",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(rupiah(p.unitPrice), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}
