package com.appoxee.internal.model.response

import com.appoxee.internal.util.getStringOrEmpty
import com.appoxee.internal.util.toList
import org.json.JSONObject

internal data class RegisterPayload(
    val dmcUserId: String,
    val alias: String?,
) {

    fun toJSON(): JSONObject {
        return JSONObject().apply {
            put("dmcUserId", dmcUserId)
            put("alias", alias)
        }
    }

    companion object {
        fun fromJSON(json: JSONObject): RegisterPayload {
            val dmcUserId = json.optString("dmcUserId")
            val register =
                json.optJSONArray("register")?.toList().orEmpty().filter { dmcUserId != it }
            return RegisterPayload(
                dmcUserId = json.getStringOrEmpty("dmcUserId"),
                alias = register.firstOrNull()
            )
        }
    }
}