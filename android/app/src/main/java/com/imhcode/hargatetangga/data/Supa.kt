package com.imhcode.hargatetangga.data

import com.imhcode.hargatetangga.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

// The app only holds the Supabase URL and anon key. The Meta API key stays in Edge Function secrets.
object Supa {
    val isConfigured: Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    val client: SupabaseClient by lazy {
        createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) {
            // Reading a receipt can take 10-25 s (two model calls, sometimes a second read).
            requestTimeout = 90.seconds
            install(Auth)
            install(Functions)
            install(Postgrest)
        }
    }

    val json = Json { ignoreUnknownKeys = true }
}
