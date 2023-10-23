package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import org.json.JSONObject

internal data class BaseBodyModel(
    private val key: String,
    private val actions: NetworkData
) :
    NetworkData {
    override fun asJson(): JSONObject {
        return JSONObject()
            .put("key", key)
            .put("actions", actions.asJson())
    }

    override fun asString(): String {
        return asJson().toString()
    }
}