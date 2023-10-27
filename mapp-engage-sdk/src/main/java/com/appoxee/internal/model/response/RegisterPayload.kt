package com.appoxee.internal.model.response

import com.appoxee.internal.util.getNullableString
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
        dmcUserId = json.getNullableString("dmcUserId") ?: ""
        if (json.has("register")) {
            val register = json.getJSONArray("register").toList().filter { it != dmcUserId }
            alias = register.firstOrNull() ?: ""
        }
    }
}