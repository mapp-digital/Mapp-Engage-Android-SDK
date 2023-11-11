package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.UUID

internal open class AttributesGet(
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