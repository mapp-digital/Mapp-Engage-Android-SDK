package com.appoxee.internal.model.response

import com.appoxee.internal.util.toList
import org.json.JSONObject

internal data class RegisterPayload(
    private val json: JSONObject
) {
    var dmcUserId: String = ""
        private set
    var alias: String = ""
        private set

    init {
        if (json.has("dmcUserId")) {
            dmcUserId = json.getString("dmcUserId")
        }

        if (json.has("register")) {
            val register = json.getJSONArray("register").toList().filter { it != dmcUserId }
            alias = register.firstOrNull() ?: ""
        }
    }
}