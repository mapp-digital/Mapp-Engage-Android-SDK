package com.appoxee.internal.model.request.events

import com.appoxee.internal.model.request.events.ClickType.Companion.numeric
import com.appoxee.internal.network.NetworkData
import org.json.JSONObject

internal class PushEvent(
    private val tenantId: String,
    private val eventType: EventType,
    private val messageId: Long,
    private val dmcUserId: String,
    private val sendoutId: Long,
    private val clickType: ClickType?
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
                // backend accepts only values from 0 to 4 included.
                clickType?.numeric()?.let {
                    put("click_action_type", it)
                }
            }
        }
        return json
    }

    override fun asString(): String {
        return asJson().toString()
    }
}