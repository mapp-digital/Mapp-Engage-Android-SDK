package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import org.json.JSONObject

internal class OptInModel(private val pushToken: String) : NetworkData {
    private lateinit var json: JSONObject
    override fun asJson(): JSONObject {
        if (!::json.isInitialized) {
            json = JSONObject().apply {
                put("set", JSONObject().apply {
                    put("pushToken", pushToken)
                    put("pushToken_bk", "")
                })
            }
        }
        return json
    }

    override fun asString(): String {
        return asJson().toString()
    }
}