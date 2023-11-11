package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import org.json.JSONObject
import java.util.Date
import java.util.UUID

internal class RequestBody(
    private val key: String,
    private val actions: NetworkData,
    private val alias: String? = null
) :
    NetworkData {
    private lateinit var json: JSONObject
    private val time = Date().time.toString()
    private val requestId = UUID.randomUUID().toString()
    override fun asJson(): JSONObject {
        if (!::json.isInitialized) {
            json = JSONObject().apply {
                put("key", key)
                alias?.let { put("alias", it) }
                put("actions", actions.asJson().apply {
                    put("time", time)
                    put("requestId", requestId)
                })
            }

        }
        return json
    }

    override fun asString(): String {
        return asJson().toString()
    }
}