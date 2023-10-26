package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import org.json.JSONArray
import org.json.JSONObject

internal data class SetAliasModel(val alias: String) : NetworkData {
    override fun asJson(): JSONObject {
        val json = JSONObject().apply {
            put("set", JSONObject().apply {
                put("alias", alias)
            })
        }
        return json
    }

    override fun asString(): String {
        return asJson().toString()
    }
}