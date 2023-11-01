package com.appoxee.internal.model.response

import com.appoxee.internal.util.getNullableString
import com.appoxee.internal.util.getStringOrEmpty
import org.json.JSONObject

internal data class RegisterPayload(
    val dmcUserId: String,
    val alias: String?,
) {
    companion object {
        fun fromJSON(json: JSONObject): RegisterPayload {
            return RegisterPayload(
                dmcUserId = json.getStringOrEmpty("dmcUserId"),
                alias = json.getNullableString("alias")
            )
        }
    }
}