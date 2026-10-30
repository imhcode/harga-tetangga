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
import com.imhcode.hargatetangga.data.NewTrip
import com.imhcode.hargatetangga.data.ReceiptRepository
import com.imhcode.hargatetangga.data.TripLineDto
import com.imhcode.hargatetangga.data.TripPreview
import com.imhcode.hargatetangga.data.TripRepository
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

sealed interface ReceiptState {
    data object Idle : ReceiptState
    data class Preview(val uri: Uri) : ReceiptState
    data class Reading(val uri: Uri) : ReceiptState
    data class Review(val draft: ReceiptDraft) : ReceiptState
    data class Saved(val count: Int, val corrected: Int, val receiptId: String, val trip: TripPreview?) : ReceiptState
    data class TripReview(val receiptId: String, val trip: TripPreview) : ReceiptState
    data class TripSaved(val summary: TripSummary, val bring: Double?, val receiptTotal: Double) : ReceiptState
    data class Failed(val message: String, val uri: Uri) : ReceiptState
}

// F3: one receipt line on the Trip Result screen; the person can correct the "outside the plan" mark.
class TripLineEdit(val line: TripLineDto) {
    var offPlan by mutableStateOf(line.offPlan)
    // The list item this line fulfils; the person can pick one when the automatic match missed.
    var listItemId by mutableStateOf(line.listItemId)
    var listItemLabel by mutableStateOf(line.listItemLabel)

    fun linkTo(id: String, label: String) {
        listItemId = id
        listItemLabel = label
        offPlan = false
    }
}

data class TripSummary(
    val plannedEstimated: Double,
    val plannedActual: Double,
    val itemsBought: Int,
    val itemsOffPlan: Int,
    val offPlanTotal: Double,
    val notBought: List<String>,
)

// Same rules as summarize() in supabase/functions/_shared/trip.ts.
fun summarizeTrip(trip: TripPreview, lines: List<TripLineEdit>): TripSummary {
    val bought = lines.filter { !it.offPlan }.mapNotNull { it.listItemId }.toSet()
    val quotes = trip.quotes.associateBy { it.itemId }
    var estimated = 0.0
    var actual = 0.0
    for (itemId in bought) {
        val quote = quotes[itemId] ?: continue
        estimated += quote.unitPrice * quote.quantity
        actual += lines.filter { !it.offPlan && it.listItemId == itemId }.sumOf { it.line.lineTotal }
    }
    val offPlan = lines.filter { it.offPlan }
    return TripSummary(
        plannedEstimated = estimated,
        plannedActual = actual,
        itemsBought = lines.size,
        itemsOffPlan = offPlan.size,
        offPlanTotal = offPlan.sumOf { it.line.lineTotal },
        notBought = trip.items.filter { it.id !in bought }.map { it.label },
    )
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
                // A failed comparison must not hide that the prices were saved.
                val trip = runCatching { TripRepository.preview(review.draft.receiptId) }.getOrNull()?.takeIf { it.hasList }
                state = ReceiptState.Saved(kept.size, corrected, review.draft.receiptId, trip)
            } catch (e: Exception) {
                saveError = "Gagal menyimpan: ${e.message ?: e::class.simpleName}"
            } finally {
                saving = false
            }
        }
    }

    val tripLines = mutableStateListOf<TripLineEdit>()

    fun openTrip(saved: ReceiptState.Saved) {
        openTripPreview(saved.receiptId, saved.trip ?: return)
    }

    private fun openTripPreview(receiptId: String, trip: TripPreview) {
        tripLines.clear()
        tripLines.addAll(trip.lines.map { TripLineEdit(it) })
        state = ReceiptState.TripReview(receiptId, trip)
    }

    // A receipt saved during a trip whose comparison was never finished (for example the app closed).
    var pendingTrip by mutableStateOf<TripPreview?>(null)
        private set

    fun checkPendingTrip() {
        viewModelScope.launch {
            pendingTrip = runCatching { TripRepository.preview() }.getOrNull()?.takeIf { it.hasList && it.receiptId != null }
        }
    }

    fun openPendingTrip() {
        val trip = pendingTrip ?: return
        pendingTrip = null
        openTripPreview(trip.receiptId!!, trip)
    }

    // Written only after the person checked the "outside the plan" marks.
    fun saveTrip() {
        val review = state as? ReceiptState.TripReview ?: return
        val trip = review.trip
        val summary = summarizeTrip(trip, tripLines)
        saving = true
        saveError = null
        viewModelScope.launch {
            try {
                TripRepository.save(
                    NewTrip(
                        listId = trip.listId!!,
                        estimateId = trip.estimateId,
                        receiptId = review.receiptId,
                        bring = trip.bring,
                        receiptTotal = trip.receiptTotal,
                        plannedEstimated = summary.plannedEstimated,
                        plannedActual = summary.plannedActual,
                        itemsBought = summary.itemsBought,
                        itemsOffPlan = summary.itemsOffPlan,
                        offPlanTotal = summary.offPlanTotal,
                        itemsNotBought = summary.notBought.size,
                        details = buildJsonObject {
                            put("off_plan", JsonArray(tripLines.filter { it.offPlan }.map { JsonPrimitive(it.line.rawName) }))
                            put("not_bought", JsonArray(summary.notBought.map { JsonPrimitive(it) }))
                            put("marks_changed", tripLines.count { it.offPlan != it.line.offPlan || it.listItemId != it.line.listItemId })
                        },
                    ),
                )
                tripLines.clear()
                state = ReceiptState.TripSaved(summary, trip.bring, trip.receiptTotal)
            } catch (e: Exception) {
                saveError = "Gagal menyimpan hasil belanja: ${e.message ?: e::class.simpleName}"
            } finally {
                saving = false
            }
        }
    }

    fun reset() {
        tripLines.clear()
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
