package com.appoxee.internal.model.response.inapp

import com.appoxee.internal.util.getLongOrDefault
import com.appoxee.internal.util.getStringOrEmpty
import org.json.JSONObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class WebInappMessage(
    override val templateId: String,
    override val content: String,
    override val type: InappType,
    override val behaviour: Behaviour?,
    override val location: Location?
) : Message(templateId, content, type, behaviour, location) {

    @OptIn(ExperimentalEncodingApi::class)
    val decodedHtml:String
        get() = Base64.decode(content,0,content.length).decodeToString()

    companion object {
        fun fromJSON(json: JSONObject): WebInappMessage {
            return WebInappMessage(
                templateId = json.getStringOrEmpty("template_id"),
                content = json.getStringOrEmpty("content"),
                type = InappType.from(json.getLongOrDefault("type", 0).toInt()),
                behaviour = json.getJSONObject("behaviour")?.let { Behaviour.fromJSON(it) },
                location = json.getJSONObject("location")?.let { Location.fromJSON(it) },
            )
        }
    }
}
