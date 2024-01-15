package com.appoxee.internal.model.request.events

import com.appoxee.internal.network.NetworkData
import org.json.JSONObject

internal class PushEvent(
    private val tenantId: String,
    private val eventType: NotificationClick,
    private val messageId: Long,
    private val dmcUserId: String,
    private val sendoutId: Long,
    private val clickType: PushAction?
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
                // DISMISS is not acceptable on backend, so we are omit sending that value
                if (clickType != PushAction.DISMISS) {
                    clickType?.ordinal?.let {
                        put("click_action_type", it)
                    }
                }
            }
        }
        return json
    }

    override fun asString(): String {
        return asJson().toString()
    }
}