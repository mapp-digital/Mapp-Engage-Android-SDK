package com.appoxee.internal.model.request.events

import com.appoxee.internal.network.NetworkData
import org.json.JSONObject

internal class PushEvent(
    private val tenantId: String,
    private val eventType: PushEventType,
    private val messageId: Long,
    private val dmcUserId: String,
    private val sendoutId: Long,
    private val clickType: ClickActionType
) : NetworkData {

    private lateinit var json: JSONObject
    override fun asJson(): JSONObject {
        if (!::json.isInitialized) {
            json = JSONObject().apply {
                put("customer_id", tenantId)
                put("event_type", eventType.ordinal)
                put("message_id", messageId)
                put("user_id", dmcUserId)
                put("sendout_id", sendoutId)
                put("click_action_type", clickType.ordinal)
            }
        }
        return json;
    }

    override fun asString(): String {
        return asJson().toString()
    }
}