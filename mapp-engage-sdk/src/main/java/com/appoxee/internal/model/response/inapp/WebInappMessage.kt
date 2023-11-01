package com.appoxee.internal.model.response.inapp

import com.appoxee.internal.util.getLongOrDefault
import com.appoxee.internal.util.getStringOrEmpty
import org.json.JSONObject

data class WebInappMessage(
    val templateId: String,
    val content: String,
    val type: Int,
    val behaviour: Behaviour?,
    val location: Location?
) {
    companion object {
        fun fromJSON(json: JSONObject): WebInappMessage {
            return WebInappMessage(
                templateId = json.getStringOrEmpty("template_id"),
                content = json.getStringOrEmpty("content"),
                type = json.getLongOrDefault("type", 0).toInt(),
                behaviour = json.getJSONObject("behaviour")?.let { Behaviour.fromJSON(it) },
                location = json.getJSONObject("location")?.let { Location.fromJSON(it) },
            )
        }
    }
}
