package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import org.json.JSONObject
import java.util.Date
import java.util.UUID

internal data class ActivationModel(val timeSpent: Long) : NetworkData {
    private lateinit var json: JSONObject
    override fun asJson(): JSONObject {
        if (!::json.isInitialized) {
            json = JSONObject().apply {
                put("timeSpent", timeSpent.toString())
            }
        }
        return json
    }

    override fun asString(): String {
        return asJson().toString()
    }
}