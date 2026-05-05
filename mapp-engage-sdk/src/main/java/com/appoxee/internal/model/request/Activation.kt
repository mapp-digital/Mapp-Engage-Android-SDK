package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import org.json.JSONObject

internal data class Activation(val timeSpent: Long) : NetworkData {
    private lateinit var json: JSONObject
    override fun asJson(): JSONObject {
        if (!::json.isInitialized) {
            val timeSpent = JSONObject().apply {
                put("timeSpent", timeSpent.toString())
            }
            json = JSONObject().apply {
                put("activation", timeSpent)
            }
        }
        return json
    }

    override fun asString(): String {
        return asJson().toString()
    }
}