package com.appoxee.internal.model.response

import org.json.JSONObject

internal data class Metadata(private val json: JSONObject?) {
    var error: Boolean = false
        private set
    var statusCode: Int = 200
        private set

    init {
        json?.let { js ->
            if (js.has("error")) {
                error = js.getBoolean("error")
            }
            if (js.has("statusCode")) {
                statusCode = js.getInt("statusCode")
            }
        }
    }
}