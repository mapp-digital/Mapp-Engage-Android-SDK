package com.appoxee.internal.model.response

import com.appoxee.internal.util.getNullableString
import com.appoxee.internal.util.toList
import org.json.JSONObject

internal data class DefaultResponse(
    private val json: JSONObject
) {
    var dmcUserId: String = ""
        private set

    var set: List<String> = emptyList()
        private set

    init {
        dmcUserId = json.getNullableString("dmcUserId") ?: ""
        if (json.has("set")) {
            set = json.getJSONArray("set").toList()
        }
    }
}