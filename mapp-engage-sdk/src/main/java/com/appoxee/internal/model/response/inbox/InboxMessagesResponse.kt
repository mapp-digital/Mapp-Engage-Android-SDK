package com.appoxee.internal.model.response.inbox

import com.appoxee.internal.util.arrayToList
import com.appoxee.internal.util.getStringOrEmpty
import org.json.JSONObject

data class InboxMessagesResponse(val eventId: String, val messages: List<InboxMessage>) {

    companion object {
        fun fromJSON(json: JSONObject, eventKey: String): InboxMessagesResponse {
            val eventId = json.getStringOrEmpty("event_id")
            val messages = json.arrayToList("messages") {
                InboxMessage.fromJSON(it, eventId, eventKey)
            }
            return InboxMessagesResponse(
                eventId = eventId,
                messages = messages.sortedByDescending { it.templateId }
            )
        }
    }
}