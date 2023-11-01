package com.appoxee.internal.model.response.inbox

import com.appoxee.internal.util.arrayToList
import com.appoxee.internal.util.getStringOrEmpty
import org.json.JSONObject

data class InboxMessagesResponse(val eventId: String, val messages: List<InboxMessage>) {

    companion object {
        fun fromJSON(json: JSONObject): InboxMessagesResponse {
            return InboxMessagesResponse(
                eventId = json.getStringOrEmpty("event_id"),
                messages = json.arrayToList("messages") {
                    InboxMessage.fromJSON(it)
                }
            )
        }
    }
}