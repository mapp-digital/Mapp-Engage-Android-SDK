package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import org.json.JSONObject

data class MessageBody(
    val timestamp: Long,
    val id: String,
    val userId: String,
    val alias: String,
    val eventKey: String,
    val deviceId:String,
) : NetworkData {
    private lateinit var json: JSONObject
    override fun asJson(): JSONObject {
        if (!::json.isInitialized) {
            json = JSONObject().apply {
                put("timestamp", timestamp)
                put("id", id)
                put("user_id", userId)
                put("alias", alias)
                put("event_key", eventKey)
                put("device_id",deviceId)
            }
        }
        return json
    }

    override fun asString(): String {
        return asJson().toString()
    }

}
