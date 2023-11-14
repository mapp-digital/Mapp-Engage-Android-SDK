package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import org.json.JSONArray
import org.json.JSONObject

internal open class GetAttributes(
    private val attributes: List<String>,
) :
    NetworkData {
    private lateinit var json: JSONObject
    override fun asJson(): JSONObject {
        if (!::json.isInitialized) {
            json = JSONObject().apply {
                put("get", JSONArray(attributes))
            }
        }
        return json
    }

    override fun asString(): String {
        return asJson().toString()
    }
}