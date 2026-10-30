package com.imhcode.hargatetangga.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// F5: household membership. Every change goes through a database function; the app never writes
// household rows directly.

@Serializable
data class HouseholdRow(
    val id: String,
    val name: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("invite_code") val inviteCode: String,
)

@Serializable
data class MemberRow(
    @SerialName("household_id") val householdId: String,
    @SerialName("user_id") val userId: String,
    val email: String? = null,
    val role: String,
    val status: String,
)

data class HouseholdState(
    val me: String,
    val myMembership: MemberRow?,
    val household: HouseholdRow?,
    val members: List<MemberRow>,
) {
    val isOwner get() = household != null && household.ownerId == me
}

object HouseholdRepository {
    suspend fun load(): HouseholdState {
        val me = Supa.client.auth.currentUserOrNull()?.id ?: error("Belum masuk")
        val members = Supa.client.postgrest.from("household_members").select {
            order("requested_at", Order.ASCENDING)
        }.decodeList<MemberRow>()
        val household = Supa.client.postgrest.from("households").select().decodeList<HouseholdRow>().firstOrNull()
        return HouseholdState(me, members.firstOrNull { it.userId == me }, household, members)
    }

    suspend fun create(name: String) {
        Supa.client.postgrest.rpc("create_household", buildJsonObject { put("p_name", name) })
    }

    suspend fun requestJoin(code: String) {
        Supa.client.postgrest.rpc("request_join", buildJsonObject { put("p_code", code) })
    }

    // H7: approve or reject a pending member. Only the owner's call succeeds.
    suspend fun decide(userId: String, approve: Boolean) {
        Supa.client.postgrest.rpc("decide_member", buildJsonObject {
            put("p_user_id", userId)
            put("p_approve", approve)
        })
    }

    suspend fun leave() {
        Supa.client.postgrest.rpc("leave_household")
    }
}
