package com.imhcode.hargatetangga.ui

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.imhcode.hargatetangga.data.CatalogProduct
import com.imhcode.hargatetangga.data.ConfirmLine
import com.imhcode.hargatetangga.data.DraftLine
import com.imhcode.hargatetangga.data.DraftStore
import com.imhcode.hargatetangga.data.NewProduct
import com.imhcode.hargatetangga.data.ReceiptDraft
import com.imhcode.hargatetangga.data.ReceiptRepository
import kotlinx.coroutines.launch
import java.io.File

sealed interface ReceiptState {
    data object Idle : ReceiptState
    data class Preview(val uri: Uri) : ReceiptState
    data class Reading(val uri: Uri) : ReceiptState
    data class Review(val draft: ReceiptDraft) : ReceiptState
    data class Saved(val count: Int, val corrected: Int) : ReceiptState
    data class Failed(val message: String, val uri: Uri) : ReceiptState
}

// One receipt line as the person edits it on the Review screen.
class LineEdit(val line: DraftLine) {
    val initialName: String = line.productName ?: line.suggestion?.name ?: line.rawName
    var productId by mutableStateOf(line.productId)
    var name by mutableStateOf(initialName)
    var quantity by mutableStateOf(line.quantity.toInput())
    var unitPrice by mutableStateOf(line.unitPrice.toInput())
    var lineTotal by mutableStateOf(line.lineTotal.toInput())
    var skip by mutableStateOf(false)
    var open by mutableStateOf(!line.autoAccept)

    // Typing an existing product's exact name links to it; anything else becomes a new product.
    fun rename(text: String, catalog: List<CatalogProduct>) {
        name = text
        productId = catalog.firstOrNull { it.name.equals(text.trim(), ignoreCase = true) }?.id
    }

    fun choose(product: CatalogProduct) {
        name = product.name
        productId = product.id
    }

    val isValid: Boolean
        get() = skip || (name.isNotBlank() && (parsePrice(unitPrice) ?: 0.0) > 0)

    val corrected: Boolean
        get() = skip ||
            productId != line.productId ||
            (productId == null && name.trim() != initialName) ||
            parseQuantity(quantity) != line.quantity ||
            parsePrice(unitPrice) != line.unitPrice ||
            parsePrice(lineTotal) != line.lineTotal

    fun toConfirmLine(): ConfirmLine {
        val suggestion = line.suggestion
        val newProduct = when {
            productId != null -> null
            suggestion != null && name.trim() == suggestion.name -> suggestion
            // Renamed by the person: keep brand and category, drop the size the model guessed.
            else -> NewProduct(name = name.trim(), brand = suggestion?.brand, category = suggestion?.category ?: "Lainnya")
        }
        return ConfirmLine(
            rawName = line.rawName,
            productId = productId,
            newProduct = newProduct,
            quantity = parseQuantity(quantity),
            unitPrice = parsePrice(unitPrice) ?: 0.0,
            lineTotal = parsePrice(lineTotal),
            autoAccepted = line.autoAccept && !corrected,
            corrected = corrected,
        )
    }
}

class ReceiptViewModel(app: Application) : AndroidViewModel(app) {
    var state by mutableStateOf<ReceiptState>(ReceiptState.Idle)
        private set
    var store by mutableStateOf(DraftStore())
    var purchaseDate by mutableStateOf("")
    var saving by mutableStateOf(false)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set
    val edits = mutableStateListOf<LineEdit>()
    private var reviewStartedAt = 0L

    private val photoDir get() = File(getApplication<Application>().cacheDir, "receipts")

    fun newCaptureUri(): Uri {
        val file = File(photoDir.apply { mkdirs() }, "receipt-${System.currentTimeMillis()}.jpg")
        val app = getApplication<Application>()
        return FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
    }

    fun onPhoto(uri: Uri) {
        state = ReceiptState.Preview(uri)
    }

    // H1: the photo leaves the phone only after the person pressed "Kirim ke AI".
    fun send(uri: Uri) {
        state = ReceiptState.Reading(uri)
        viewModelScope.launch {
            try {
                val image = ReceiptRepository.encodeReceiptImage(getApplication(), uri)
                val draft = ReceiptRepository.extract(image)
                store = draft.store
                purchaseDate = draft.purchaseDate ?: ""
                edits.clear()
                edits.addAll(draft.lines.filter { it.kind == "item" }.map { LineEdit(it) })
                saveError = null
                reviewStartedAt = SystemClock.elapsedRealtime()
                state = ReceiptState.Review(draft)
            } catch (e: Exception) {
                state = ReceiptState.Failed("Struk gagal dibaca: ${e.message ?: e::class.simpleName}", uri)
            }
        }
    }

    fun chooseStore(existing: DraftStore) {
        store = existing
    }

    fun editStore(name: String? = null, branch: String? = null, kind: String? = null) {
        // Any edit detaches from a saved store; confirm_receipt then finds or creates it by name and branch.
        store = store.copy(
            id = null,
            name = name ?: store.name,
            branch = branch ?: store.branch,
            kind = kind ?: store.kind,
        )
    }

    val canSave: Boolean
        get() = store.name.isNotBlank() && edits.any { !it.skip } && edits.all { it.isValid }

    // H2/H3: prices and aliases are written only here, after the person reviewed every line.
    fun save() {
        val review = state as? ReceiptState.Review ?: return
        saving = true
        saveError = null
        viewModelScope.launch {
            try {
                val kept = edits.filter { !it.skip }
                ReceiptRepository.confirm(
                    receiptId = review.draft.receiptId,
                    store = store,
                    purchaseDate = purchaseDate.ifBlank { null },
                    lines = kept.map { it.toConfirmLine() },
                    verifyMs = SystemClock.elapsedRealtime() - reviewStartedAt,
                )
                val corrected = edits.count { it.corrected }
                clearPhotos()
                edits.clear()
                state = ReceiptState.Saved(kept.size, corrected)
            } catch (e: Exception) {
                saveError = "Gagal menyimpan: ${e.message ?: e::class.simpleName}"
            } finally {
                saving = false
            }
        }
    }

    fun reset() {
        clearPhotos()
        edits.clear()
        saveError = null
        state = ReceiptState.Idle
    }

    // Camera photos are only needed until the receipt is read and saved.
    private fun clearPhotos() {
        photoDir.listFiles()?.forEach { it.delete() }
    }
}
