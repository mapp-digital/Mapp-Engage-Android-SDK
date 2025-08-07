package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import org.json.JSONObject

data class UpdateDevice(private val params: Map<String, String>) : NetworkData {
    override fun asJson(): JSONObject {
        return JSONObject().apply {
            put("set", JSONObject().apply {
                for ((key, value) in params) {
                    put(key, value)
                }
            })
        }
    }

    override fun asString(): String {
        return asJson().toString()
    }

}
