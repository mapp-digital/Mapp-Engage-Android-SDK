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
            return InappResponse(
                eventId = json.getStringOrEmpty("event_id"),
                eventKey = json.getStringOrEmpty("event_key"),
                webMessages = json.arrayToList("web_messages") {
                    WebInappMessage.fromJSON(it)
                },
                nativeMessages = json.arrayToList("native_messages") {
                    NativeInappMessage.fromJSON(it)
                }
            )
        }
    }
}
