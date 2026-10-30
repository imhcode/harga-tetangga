package com.imhcode.hargatetangga.data

import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

object ShoppingRepository {

    private suspend inline fun <reified T> callFunction(name: String, body: JsonObject): T {
        val response = Supa.client.functions.invoke(function = name, body = body)
        return Supa.json.decodeFromString(response.bodyAsText())
    }

    // The open list (not yet finished), or a new one.
    suspend fun currentList(): ShoppingListRow {
        val open = Supa.client.postgrest.from("shopping_lists").select {
            filter { neq("status", "done") }
            order("created_at", Order.DESCENDING)
            limit(1)
        }.decodeList<ShoppingListRow>()
        return open.firstOrNull() ?: Supa.client.postgrest.from("shopping_lists")
            .insert(buildJsonObject { put("name", "Belanja") }) { select() }
            .decodeSingle<ShoppingListRow>()
    }

    suspend fun items(listId: String): List<ListItemRow> =
        Supa.client.postgrest.from("shopping_list_items").select {
            filter { eq("list_id", listId) }
            order("created_at", Order.ASCENDING)
        }.decodeList<ListItemRow>()

    suspend fun parse(text: String): ParseResult =
        callFunction("parse-list", buildJsonObject { put("text", text) })

    // F4-03: a recorded WAV becomes the same items to tick as a typed list.
    suspend fun parseVoice(wav: ByteArray): ParseResult =
        callFunction("voice-list", buildJsonObject {
            put("audio_base64", android.util.Base64.encodeToString(wav, android.util.Base64.NO_WRAP))
        })

    // H4/H5: called only with items the person ticked or accepted.
    suspend fun addItems(items: List<NewListItem>) {
        if (items.isNotEmpty()) Supa.client.postgrest.from("shopping_list_items").insert(items)
    }

    suspend fun deleteItem(id: String) {
        Supa.client.postgrest.from("shopping_list_items").delete { filter { eq("id", id) } }
    }

    suspend fun estimate(listId: String, bufferPct: Int, save: Boolean = false): EstimateDto =
        callFunction("estimate", buildJsonObject {
            put("list_id", listId)
            put("buffer_pct", bufferPct)
            put("save", save)
        })

    suspend fun finishList(listId: String) {
        Supa.client.postgrest.from("shopping_lists").update(buildJsonObject { put("status", "done") }) {
            filter { eq("id", listId) }
        }
    }

    suspend fun ask(listId: String, message: String, history: List<ChatMessage>): AgentReply =
        callFunction("agent", buildJsonObject {
            put("list_id", listId)
            put("message", message)
            put("history", Supa.json.encodeToJsonElement(history))
        })
}
