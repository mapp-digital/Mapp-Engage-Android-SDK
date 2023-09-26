package com.appoxee.internal.model.response

import org.json.JSONObject

internal data class Metadata(var error: Boolean, var statusCode: Int) {
    constructor(json: JSONObject?) : this(false, 0) {
        error = if (json?.has("error") == true) json.getBoolean("error") else false
        statusCode = if (json?.has("statusCode") == true) json.getInt("statusCode") else 0
    }
}