package com.imhcode.hargatetangga.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ReceiptScreen(onShowPrices: () -> Unit, vm: ReceiptViewModel = viewModel()) {
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) pendingCapture?.let { vm.onPhoto(Uri.parse(it)) }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(vm::onPhoto)
    }
    val capture = {
        val uri = vm.newCaptureUri()
        pendingCapture = uri.toString()
        takePicture.launch(uri)
    }
    val pick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    when (val state = vm.state) {
        ReceiptState.Idle -> Centered {
            LaunchedEffect(Unit) { vm.checkPendingTrip() }
            Text("Foto struk", style = MaterialTheme.typography.headlineMedium)
            vm.pendingTrip?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Struk dari belanja tadi belum dibandingkan dengan rencana.", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = vm::openPendingTrip, modifier = Modifier.fillMaxWidth()) { Text("Bandingkan dengan rencana") }
                    }
                }
            }
            Text(
                "Harga dari struk disimpan per toko setelah kamu periksa. Foto struk utuh dari atas sampai total.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = capture, modifier = Modifier.fillMaxWidth()) { Text("Foto struk") }
            OutlinedButton(onClick = pick, modifier = Modifier.fillMaxWidth()) { Text("Pilih dari galeri") }
        }

        is ReceiptState.Preview -> Centered {
            Text("Kirim foto ini?", style = MaterialTheme.typography.headlineSmall)
            Thumbnail(state.uri)
            Text(
                "Foto dikirim ke Meta AI untuk dibaca. Pastikan tidak ada kartu, nomor member, atau data pribadi lain yang terlihat.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = { vm.send(state.uri) }, modifier = Modifier.fillMaxWidth()) { Text("Kirim ke AI") }
            OutlinedButton(onClick = { vm.reset() }, modifier = Modifier.fillMaxWidth()) { Text("Ganti foto") }
        }

        is ReceiptState.Reading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator()
                Text("Membaca struk…", style = MaterialTheme.typography.titleMedium)
                Text("Biasanya 10–20 detik", style = MaterialTheme.typography.bodyMedium)
            }
        }

        is ReceiptState.Review -> ReviewScreen(vm, state.draft)

        is ReceiptState.Saved -> Centered {
            Text("Tersimpan", style = MaterialTheme.typography.headlineMedium)
            Text(
                "${state.count} harga disimpan" + if (state.corrected > 0) ", ${state.corrected} baris kamu koreksi." else ".",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (state.trip != null) {
                Text(
                    "Kamu sedang belanja dengan daftar. Bandingkan struk ini dengan rencananya.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = { vm.openTrip(state) }, modifier = Modifier.fillMaxWidth()) { Text("Bandingkan dengan rencana") }
                OutlinedButton(onClick = onShowPrices, modifier = Modifier.fillMaxWidth()) { Text("Lihat harga") }
            } else {
                Button(onClick = onShowPrices, modifier = Modifier.fillMaxWidth()) { Text("Lihat harga") }
            }
            OutlinedButton(onClick = { vm.reset() }, modifier = Modifier.fillMaxWidth()) { Text("Foto struk lain") }
        }

        is ReceiptState.TripReview -> TripReviewScreen(vm, state.trip)

        is ReceiptState.TripSaved -> Centered {
            Text("Hasil belanja tersimpan", style = MaterialTheme.typography.headlineSmall)
            CashLine(state.bring, state.receiptTotal)
            TripNumbers(state.summary)
            Text("Daftar belanja ditutup. Daftar baru siap di tab Belanja.", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { vm.reset() }, modifier = Modifier.fillMaxWidth()) { Text("Selesai") }
        }

        is ReceiptState.Failed -> Centered {
            Text("Belum berhasil", style = MaterialTheme.typography.headlineSmall)
            Text(state.message, color = MaterialTheme.colorScheme.error)
            Button(onClick = { vm.send(state.uri) }, modifier = Modifier.fillMaxWidth()) { Text("Coba lagi") }
            TextButton(onClick = { vm.reset() }) { Text("Ganti foto") }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) { content() }
}

@Composable
private fun Thumbnail(uri: Uri) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                    ?.asImageBitmap()
            }.getOrNull()
        }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = "Foto struk",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
        )
    }
}
