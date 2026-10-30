package com.imhcode.hargatetangga.ui

import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToLong

private val idNumber = NumberFormat.getNumberInstance(Locale("id", "ID"))

fun rupiah(value: Double?): String = if (value == null) "Rp ?" else "Rp ${idNumber.format(value.roundToLong())}"

fun quantityText(value: Double?): String = when {
    value == null -> "?"
    value % 1.0 == 0.0 -> value.toLong().toString()
    else -> value.toString().replace('.', ',')
}

// Value shown in an editable number field.
fun Double?.toInput(): String = when {
    this == null -> ""
    this % 1.0 == 0.0 -> this.toLong().toString()
    else -> this.toString()
}

// Prices are whole rupiah; "12.500" and "12,500" both mean 12500.
fun parsePrice(text: String): Double? = text.filter { it.isDigit() }.toDoubleOrNull()

// Quantities can be weights such as "0,5".
fun parseQuantity(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

fun daysAgo(isoDate: String): String {
    val date = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return isoDate
    val days = ChronoUnit.DAYS.between(date, LocalDate.now())
    return when {
        days <= 0 -> "hari ini"
        days == 1L -> "kemarin"
        days < 60 -> "$days hari lalu"
        else -> isoDate
    }
}

val FLAG_LABELS = mapOf(
    "low_confidence" to "AI kurang yakin",
    "missing_price" to "Harga tidak terbaca",
    "qty_price_mismatch" to "Jumlah × harga tidak cocok",
    "price_out_of_range" to "Harga tidak wajar",
    "price_jump" to "Beda jauh dari harga terakhir",
    "new_product" to "Produk baru",
    "unsure_match" to "Pencocokan belum pasti",
)

val STORE_KINDS = listOf(
    "minimarket" to "Minimarket",
    "supermarket" to "Supermarket",
    "grosir" to "Grosir",
    "warung" to "Warung",
    "lainnya" to "Lainnya",
)
