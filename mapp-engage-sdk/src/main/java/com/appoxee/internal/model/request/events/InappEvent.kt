package com.appoxee.internal.model.request.events

import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.network.NetworkData
import org.json.JSONObject
import java.util.Date
import java.util.UUID

internal class InappEvent(
    private val device: DevicePayload,
    private val messageContext: MessageContext,
    private val tracking: Tracking,

    ) : NetworkData {
    private lateinit var json: JSONObject
    override fun asJson(): JSONObject {
        if (!::json.isInitialized) {
            json = JSONObject().apply {
                put("alias", device.alias)
                put("device_id", device.udidHashed)
                put("id", UUID.randomUUID().toString())
                put("user_id", device.dmcUserId)
                put("push_enabled", !device.pushToken.isNullOrEmpty())
                put("timestamp", Date().time)
                put("message_context", messageContext.asJson())
                put("tracking", tracking.asJson())
            }
        }
        return json
    }

    override fun asString(): String {
        return json.toString()
    }
}

internal class Tracking(
    private val trackingKey: TrackingKey,
    private val trackingAttributes: Map<String, *> = emptyMap<String,Any>()
) : NetworkData {
    private lateinit var json: JSONObject
    override fun asJson(): JSONObject {
        if (!::json.isInitialized) {
            json = JSONObject().apply {
                put("tracking_attributes", JSONObject().apply {
                    trackingAttributes.entries.forEach {
                        put(it.key, it.value)
                    }
                })
                put("tracking_key", trackingKey.key)
            }
        }
        return json
    }

    override fun asString(): String {
        return json.toString()
    }

}

internal class MessageContext(
    private val originalEventId: String /* it from InApp message */,
    private val templateId: Long
) :
    NetworkData {
    private lateinit var json: JSONObject
    override fun asJson(): JSONObject {
        if (!::json.isInitialized) {
            json = JSONObject().apply {
                put("original_event_id", originalEventId)
                put("template_id", templateId)
            }
        }
        return json
    }

    override fun asString(): String {
        return json.toString()
    }
}