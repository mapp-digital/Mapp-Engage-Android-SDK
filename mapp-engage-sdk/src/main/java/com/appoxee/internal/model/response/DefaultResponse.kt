package com.appoxee.internal.model.response

import com.appoxee.internal.util.arrayToList
import com.appoxee.internal.util.getStringOrEmpty
import org.json.JSONObject

internal data class DefaultResponse(
    val dmcUserId: String,
    val set: List<String>
) {
    companion object {
        fun fromJSON(json: JSONObject): DefaultResponse {
            return DefaultResponse(
                dmcUserId = json.getStringOrEmpty("dmcUserId"),
                set = json.arrayToList("set") { it.toString() }
            )
        }
    }
}