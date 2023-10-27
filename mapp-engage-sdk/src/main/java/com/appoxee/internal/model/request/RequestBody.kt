package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import org.json.JSONObject
import java.util.Date
import java.util.UUID

internal data class RequestBody(
    private val key: String,
    private val actions: NetworkData
) :
    NetworkData {
    private lateinit var json: JSONObject
    private val time = Date().time.toString()
    private val requestId = UUID.randomUUID().toString()
    override fun asJson(): JSONObject {
        if (!::json.isInitialized) {
            json = JSONObject()
                .put("key", key)
                .put("actions", actions.asJson().apply {
                    put("time", time)
                    put("requestId", requestId)
                })
        }
        return json
    }

    override fun asString(): String {
        return asJson().toString()
    }
}