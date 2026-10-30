package com.imhcode.hargatetangga.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
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

    // Upright (EXIF-rotated) photo with its long side at most maxSide. Preview and upload use the same
    // orientation, so boxes drawn on the preview land on the same spot in the uploaded image.
    suspend fun decodeUpright(context: Context, uri: Uri, maxSide: Int): Bitmap = withContext(Dispatchers.IO) {
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
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    }

    data class EncodedImage(val base64: String, val width: Int, val height: Int, val redactions: Int)

    // F1-03 + F4-05: shrink to 1600 px (keeps small print legible) and paint the person's boxes solid black
    // before anything leaves the phone. Boxes are fractions (0..1) of the upright image.
    suspend fun encodeReceiptImage(context: Context, uri: Uri, boxes: List<RectF>, maxSide: Int = 1600): EncodedImage {
        val upright = decodeUpright(context, uri, maxSide)
        return withContext(Dispatchers.Default) {
            val bitmap = if (boxes.isEmpty()) upright else upright.copy(Bitmap.Config.ARGB_8888, true).also { mutable ->
                val canvas = Canvas(mutable)
                val paint = Paint().apply { color = Color.BLACK }
                boxes.forEach { b ->
                    canvas.drawRect(b.left * mutable.width, b.top * mutable.height, b.right * mutable.width, b.bottom * mutable.height, paint)
                }
            }
            val bytes = ByteArrayOutputStream().use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it)
                it.toByteArray()
            }
            EncodedImage(Base64.encodeToString(bytes, Base64.NO_WRAP), bitmap.width, bitmap.height, boxes.size)
        }
    }

    suspend fun extract(image: EncodedImage): ReceiptDraft {
        val response = Supa.client.functions.invoke(
            function = "extract-receipt",
            body = buildJsonObject {
                put("image_base64", image.base64)
                put("mime", "image/jpeg")
                put("width", image.width)
                put("height", image.height)
                put("redactions", image.redactions)
            },
        )
        return Supa.json.decodeFromString(response.bodyAsText())
    }

    // F4-05: the egress log shown in the Akun tab.
    suspend fun sentLog(): List<SentLogRow> =
        Supa.client.postgrest.from("receipts").select(
            io.github.jan.supabase.postgrest.query.Columns.list("created_at", "sent_bytes", "sent_width", "sent_height", "redactions", "model", "status"),
        ) {
            order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            limit(10)
        }.decodeList<SentLogRow>()

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
