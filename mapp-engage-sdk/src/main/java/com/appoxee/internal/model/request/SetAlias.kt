package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import org.json.JSONObject

internal data class SetAlias(val alias: String) : NetworkData {
    private lateinit var json: JSONObject
    override fun asJson(): JSONObject {
        if (!::json.isInitialized) {
            json = JSONObject().apply {
                put("set", JSONObject().apply {
                    put("alias", alias)
                })
            }
        }
        return json
    }

    override fun asString(): String {
        return asJson().toString()
    }
}