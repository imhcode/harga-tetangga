package com.imhcode.hargatetangga.data

import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Mirrors the trip-result Edge Function and the trips table (F3).

@Serializable
data class TripLineDto(
    @SerialName("observation_id") val observationId: String,
    @SerialName("raw_name") val rawName: String,
    @SerialName("product_name") val productName: String,
    val quantity: Double? = null,
    @SerialName("line_total") val lineTotal: Double,
    @SerialName("list_item_id") val listItemId: String? = null,
    @SerialName("list_item_label") val listItemLabel: String? = null,
    @SerialName("off_plan") val offPlan: Boolean = false,
)

@Serializable
data class PlannedItemDto(val id: String, val label: String)

@Serializable
data class PlanQuoteDto(
    @SerialName("item_id") val itemId: String,
    @SerialName("unit_price") val unitPrice: Double,
    val quantity: Double,
)

@Serializable
data class TripPreview(
    @SerialName("has_list") val hasList: Boolean = false,
    @SerialName("receipt_id") val receiptId: String? = null,
    @SerialName("list_id") val listId: String? = null,
    @SerialName("estimate_id") val estimateId: String? = null,
    val bring: Double? = null,
    @SerialName("receipt_total") val receiptTotal: Double = 0.0,
    val items: List<PlannedItemDto> = emptyList(),
    val quotes: List<PlanQuoteDto> = emptyList(),
    val lines: List<TripLineDto> = emptyList(),
)

@Serializable
data class NewTrip(
    @SerialName("list_id") val listId: String,
    @SerialName("estimate_id") val estimateId: String?,
    @SerialName("receipt_id") val receiptId: String,
    val bring: Double?,
    @SerialName("receipt_total") val receiptTotal: Double,
    @SerialName("planned_estimated") val plannedEstimated: Double,
    @SerialName("planned_actual") val plannedActual: Double,
    @SerialName("items_bought") val itemsBought: Int,
    @SerialName("items_off_plan") val itemsOffPlan: Int,
    @SerialName("off_plan_total") val offPlanTotal: Double,
    @SerialName("items_not_bought") val itemsNotBought: Int,
    val details: JsonElement,
)

object TripRepository {
    // receiptId null: the newest confirmed receipt since leaving that has not been compared yet.
    suspend fun preview(receiptId: String? = null): TripPreview {
        val response = Supa.client.functions.invoke(
            function = "trip-result",
            body = buildJsonObject { if (receiptId != null) put("receipt_id", receiptId) },
        )
        return Supa.json.decodeFromString(response.bodyAsText())
    }

    // Saves the trip the person checked and closes the list it belonged to.
    suspend fun save(trip: NewTrip) {
        Supa.client.postgrest.from("trips").insert(trip)
        ShoppingRepository.finishList(trip.listId)
    }
}
