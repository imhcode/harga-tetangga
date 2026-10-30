package com.imhcode.hargatetangga.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors the extract-receipt Edge Function response.

@Serializable
data class DraftStore(
    val id: String? = null,
    val name: String = "",
    val branch: String? = null,
    val kind: String = "lainnya",
)

@Serializable
data class NewProduct(
    val name: String,
    val brand: String? = null,
    @SerialName("size_value") val sizeValue: Double? = null,
    @SerialName("size_unit") val sizeUnit: String? = null,
    val category: String = "Lainnya",
)

@Serializable
data class DraftLine(
    val index: Int,
    val kind: String,
    @SerialName("raw_name") val rawName: String,
    val quantity: Double? = null,
    @SerialName("unit_price") val unitPrice: Double? = null,
    @SerialName("line_total") val lineTotal: Double? = null,
    val confidence: String = "high",
    val flags: List<String> = emptyList(),
    @SerialName("product_id") val productId: String? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("matched_via") val matchedVia: String = "none",
    val suggestion: NewProduct? = null,
    @SerialName("auto_accept") val autoAccept: Boolean = false,
)

@Serializable
data class CatalogProduct(val id: String, val name: String)

@Serializable
data class ReceiptDraft(
    @SerialName("receipt_id") val receiptId: String,
    val store: DraftStore,
    @SerialName("purchase_date") val purchaseDate: String? = null,
    @SerialName("printed_total") val printedTotal: Double? = null,
    @SerialName("line_sum") val lineSum: Double = 0.0,
    @SerialName("sum_matches") val sumMatches: Boolean = false,
    val unreadable: List<String> = emptyList(),
    val effort: String = "",
    val escalated: Boolean = false,
    @SerialName("extract_ms") val extractMs: Long = 0,
    val lines: List<DraftLine> = emptyList(),
    val stores: List<DraftStore> = emptyList(),
    val catalog: List<CatalogProduct> = emptyList(),
)

// Sent to the confirm_receipt RPC after the person reviewed the draft.
@Serializable
data class ConfirmLine(
    @SerialName("raw_name") val rawName: String,
    @SerialName("product_id") val productId: String?,
    @SerialName("new_product") val newProduct: NewProduct?,
    val quantity: Double?,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("line_total") val lineTotal: Double?,
    @SerialName("auto_accepted") val autoAccepted: Boolean,
    val corrected: Boolean,
)

@Serializable
data class LatestPrice(
    @SerialName("product_id") val productId: String,
    @SerialName("product_name") val productName: String,
    val category: String? = null,
    @SerialName("store_id") val storeId: String,
    @SerialName("store_name") val storeName: String,
    @SerialName("store_branch") val storeBranch: String? = null,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("observed_on") val observedOn: String,
    val source: String = "receipt",
)

// F4-05: one photo sent to the model, as recorded by extract-receipt.
@Serializable
data class SentLogRow(
    @SerialName("created_at") val createdAt: String,
    @SerialName("sent_bytes") val sentBytes: Int? = null,
    @SerialName("sent_width") val sentWidth: Int? = null,
    @SerialName("sent_height") val sentHeight: Int? = null,
    val redactions: Int = 0,
    val model: String? = null,
    val status: String = "draft",
)
