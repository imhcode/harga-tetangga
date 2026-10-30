package com.imhcode.hargatetangga.data

import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class PingResult(
    val reply: String = "",
    val model: String = "",
    @SerialName("latency_ms") val latencyMs: Long = 0,
)

// F0-07: app -> ping-model Edge Function -> Muse Spark -> app.
suspend fun pingModel(prompt: String): PingResult {
    val response = Supa.client.functions.invoke(
        function = "ping-model",
        body = buildJsonObject { put("prompt", prompt) },
    )
    return Supa.json.decodeFromString(response.bodyAsText())
}
