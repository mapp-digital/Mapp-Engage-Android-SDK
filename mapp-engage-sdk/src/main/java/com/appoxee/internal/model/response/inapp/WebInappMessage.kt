package com.appoxee.internal.model.response.inapp

import com.appoxee.internal.util.LibraryExtensions.decode
import com.appoxee.internal.util.getLongOrDefault
import com.appoxee.internal.util.getStringOrEmpty
import org.json.JSONObject

data class WebInappMessage(
    override val originalEventId: String,
    override val originalEventKey: String,
    override val templateId: Long,
    override val content: String,
    override val type: InappType,
    override val behaviour: Behaviour?,
    override val location: Location?
) : Message(originalEventId, originalEventKey, templateId, content, type, behaviour, location) {

    companion object {
        fun fromJSON(json: JSONObject, eventId: String, eventKey: String): WebInappMessage {
            return WebInappMessage(
                originalEventId = eventId,
                originalEventKey = eventKey,
                templateId = json.getLongOrDefault("template_id"),
                content = json.getStringOrEmpty("content").decode(),
                type = InappType.from(json.getLongOrDefault("type", 0).toInt()),
                behaviour = json.optJSONObject("behaviour")?.let { Behaviour.fromJSON(it) },
                location = json.optJSONObject("location")?.let { Location.fromJSON(it) },
            )
        }
    }
}
