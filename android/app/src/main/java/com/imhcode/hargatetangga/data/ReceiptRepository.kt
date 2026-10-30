package com.imhcode.hargatetangga.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ReceiptRepository {

    // F1-03: shrink the photo before upload. Long side 1600 px keeps small receipt print legible.
    suspend fun encodeReceiptImage(context: Context, uri: Uri, maxSide: Int = 1600): String =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0) error("Foto tidak bisa dibaca")

            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: error("Foto tidak bisa dibaca")

            val rotation = resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
            val scale = maxSide.toFloat() / max(decoded.width, decoded.height)
            val matrix = Matrix().apply {
                if (rotation != 0) postRotate(rotation.toFloat())
                if (scale < 1f) postScale(scale, scale)
            }
            val bitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            val bytes = ByteArrayOutputStream().use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it)
                it.toByteArray()
            }
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }

    suspend fun extract(imageBase64: String): ReceiptDraft {
        val response = Supa.client.functions.invoke(
            function = "extract-receipt",
            body = buildJsonObject {
                put("image_base64", imageBase64)
                put("mime", "image/jpeg")
            },
        )
        return Supa.json.decodeFromString(response.bodyAsText())
    }

    suspend fun confirm(
        receiptId: String,
        store: DraftStore,
        purchaseDate: String?,
        lines: List<ConfirmLine>,
        verifyMs: Long,
    ) {
        Supa.client.postgrest.rpc(
            "confirm_receipt",
            buildJsonObject {
                put("p_receipt_id", receiptId)
                put("p_store", Supa.json.encodeToJsonElement(store))
                if (purchaseDate.isNullOrBlank()) put("p_purchase_date", JsonNull) else put("p_purchase_date", purchaseDate)
                put("p_lines", Supa.json.encodeToJsonElement(lines))
                put("p_verify_ms", verifyMs)
            },
        )
    }

    suspend fun latestPrices(): List<LatestPrice> =
        Supa.client.postgrest.from("latest_prices").select().decodeList<LatestPrice>()
}
