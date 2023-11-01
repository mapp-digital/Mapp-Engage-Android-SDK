package com.appoxee.internal.model.response

import com.appoxee.internal.util.getLongOrDefault
import org.json.JSONObject

internal data class Metadata(val error: Boolean, val statusCode: Int) {

    companion object {
        fun fromJSON(json: JSONObject): Metadata {
            return Metadata(
                error = json.getBoolean("error"),
                statusCode = json.getLongOrDefault("statusCode", 0).toInt()
            )
        }
    }
}