package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import org.json.JSONObject

internal class OptOutModel(private val pushTokenBk: String) : NetworkData {
    private lateinit var json: JSONObject
    override fun asJson(): JSONObject {
        if (!::json.isInitialized) {
            json = JSONObject().apply {
                put("set", JSONObject().apply {
                    put("pushToken", "")
                    put("pushToken_bk", pushTokenBk)
                })
            }
        }
        return json
    }

    override fun asString(): String {
        return asJson().toString()
    }
}