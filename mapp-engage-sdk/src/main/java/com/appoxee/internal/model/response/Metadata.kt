package com.appoxee.internal.model.response

import com.appoxee.internal.util.getLongOrDefault
import org.json.JSONObject

internal data class Metadata(
    val error: Boolean,
    val statusCode: Int,
    val errorMessage: String? = null,
    val shouldRetry: Boolean = false
) {

    fun toJSON(): JSONObject {
        return JSONObject().apply {
            put("error", error)
            put("statusCode", statusCode)
            put("errorMessage",errorMessage)
            put("shouldRetry", shouldRetry)
        }
    }

    companion object {
        fun fromJSON(json: JSONObject): Metadata {
            return Metadata(
                error = json.getBoolean("error"),
                statusCode = json.getLongOrDefault("statusCode", 0).toInt(),
                errorMessage = json.optString("errorMessage"),
                shouldRetry = json.optBoolean("shouldRetry", false)
            )
        }
    }
}