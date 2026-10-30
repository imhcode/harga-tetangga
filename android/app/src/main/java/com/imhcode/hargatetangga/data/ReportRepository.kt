package com.imhcode.hargatetangga.data

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.YearMonth

// F5-03/04: monthly spending from confirmed receipt lines of the whole household. No model involved.

@Serializable
data class ReportProduct(val name: String, val category: String? = null, val important: Boolean = false)

@Serializable
data class ReportRow(
    @SerialName("observed_on") val observedOn: String,
    val quantity: Double? = null,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("line_total") val lineTotal: Double? = null,
    @SerialName("product_id") val productId: String,
    val products: ReportProduct,
) {
    val spend: Double get() = lineTotal ?: (unitPrice * (quantity ?: 1.0))
}

object ReportRepository {
    suspend fun month(month: YearMonth): List<ReportRow> =
        Supa.client.postgrest.from("price_observations").select(
            Columns.raw("observed_on,quantity,unit_price,line_total,product_id,products(name,category,important)"),
        ) {
            filter {
                gte("observed_on", month.atDay(1).toString())
                lte("observed_on", month.atEndOfMonth().toString())
            }
        }.decodeList<ReportRow>()

    suspend fun setImportant(productId: String, important: Boolean) {
        Supa.client.postgrest.from("products").update(buildJsonObject { put("important", important) }) {
            filter { eq("id", productId) }
        }
    }
}
