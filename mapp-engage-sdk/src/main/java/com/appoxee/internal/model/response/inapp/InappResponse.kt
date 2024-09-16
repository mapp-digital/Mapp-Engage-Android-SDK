package com.appoxee.internal.model.response.inapp

import com.appoxee.internal.util.arrayToList
import com.appoxee.internal.util.getStringOrEmpty
import org.json.JSONObject

data class InappResponse(
    val eventId: String,
    val eventKey: String,
    val webMessages: List<WebInappMessage>,
    val nativeMessages: List<NativeInappMessage>,
) {
    companion object {
        fun fromJSON(json: JSONObject): InappResponse {
            val eventId = json.getStringOrEmpty("event_id")
            val eventKey = json.getStringOrEmpty("event_key")
            return InappResponse(
                eventId = eventId,
                eventKey = eventKey,
                webMessages = json.arrayToList("web_messages") {
                    WebInappMessage.fromJSON(it, eventId, eventKey).apply {
                    }
                },
                nativeMessages = json.arrayToList("native_messages") {
                    NativeInappMessage.fromJSON(it, eventId, eventKey)
                }
            )
        }
    }
}
