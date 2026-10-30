package com.imhcode.hargatetangga.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors the parse-list, estimate and agent Edge Functions and the shopping tables.

@Serializable
data class ParsedItem(
    val label: String,
    val quantity: Double = 1.0,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val confidence: String = "high",
)

@Serializable
data class ParseResult(val items: List<ParsedItem> = emptyList())

@Serializable
data class ShoppingListRow(val id: String, val status: String = "draft")

@Serializable
data class ListItemRow(
    val id: String,
    val label: String,
    @SerialName("product_id") val productId: String? = null,
    val quantity: Double = 1.0,
)

@Serializable
data class NewListItem(
    @SerialName("list_id") val listId: String,
    val label: String,
    @SerialName("product_id") val productId: String?,
    val quantity: Double,
    val source: String,
)

@Serializable
data class ItemQuote(
    val label: String,
    val quantity: Double,
    @SerialName("store_name") val storeName: String,
    @SerialName("unit_price") val unitPrice: Double,
    val low: Double,
    val high: Double,
    @SerialName("observed_on") val observedOn: String,
    @SerialName("age_days") val ageDays: Int,
)

@Serializable
data class MissingItem(val label: String, val reason: String)

@Serializable
data class PlanDto(
    @SerialName("store_names") val storeNames: List<String> = emptyList(),
    val goods: Double = 0.0,
    @SerialName("extra_store_cost") val extraStoreCost: Double = 0.0,
    val cost: Double = 0.0,
    val items: List<ItemQuote> = emptyList(),
    val missing: List<MissingItem> = emptyList(),
)

@Serializable
data class Savings(val amount: Double, @SerialName("versus_store") val versusStore: String)

@Serializable
data class EstimateDto(
    val plan: PlanDto? = null,
    val total: Double = 0.0,
    val low: Double = 0.0,
    val high: Double = 0.0,
    val bring: Double = 0.0,
    @SerialName("buffer_pct") val bufferPct: Int = 10,
    @SerialName("items_total") val itemsTotal: Int = 0,
    @SerialName("items_missing") val itemsMissing: Int = 0,
    val savings: Savings? = null,
    @SerialName("estimate_id") val estimateId: String? = null,
)

@Serializable
data class Proposal(val label: String, val quantity: Double = 1.0)

@Serializable
data class AgentReply(
    val reply: String = "",
    val proposals: List<Proposal> = emptyList(),
)

@Serializable
data class ChatMessage(val role: String, val content: String)
