package com.appoxee.internal.model.response.inapp

import com.appoxee.internal.util.arrayToList
import com.appoxee.internal.util.getLongOrDefault
import com.appoxee.internal.util.getNullableString
import com.appoxee.internal.util.getStringOrEmpty
import org.json.JSONObject

data class NativeInappMessage(
    override val originalEventId: String,
    override val originalEventKey: String,
    override val templateId: Long,
    override val content: String,
    override val type: InappType,
    override val behaviour: Behaviour?,
    override val location: Location?,
    val imageUrl: String?,
    val title: String,
    val titleColor: String?,
    val templateBackgroundColor: String?,
    val contentColor: String?,
    val buttons: List<InappButton>,
    val contentTemplateId: ContentTemplates
) : Message(originalEventId, originalEventKey, templateId, content, type, behaviour, location) {
    companion object {
        fun fromJSON(json: JSONObject, eventId: String, eventKey: String): NativeInappMessage {
            val templateId = json.getLongOrDefault("template_id")
            return NativeInappMessage(
                originalEventId = eventId,
                originalEventKey = eventKey,
                templateId = json.getLongOrDefault("template_id"),
                content = json.getStringOrEmpty("content"),
                type = InappType.from(json.getLongOrDefault("type", 0).toInt()),
                behaviour = json.optJSONObject("behaviour")?.let { Behaviour.fromJSON(it) },
                location = json.optJSONObject("location")?.let { Location.fromJSON(it) },
                imageUrl = json.getNullableString("imageURL"),
                title = json.getStringOrEmpty("title"),
                titleColor = json.getNullableString("title_color"),
                templateBackgroundColor = json.getNullableString("template_background_color"),
                contentColor = json.getNullableString("content_color"),
                buttons = json.arrayToList("buttons") { InappButton.fromJSON(templateId, it) },
                contentTemplateId = ContentTemplates.from(json.getStringOrEmpty("content_template_id"))
            )
        }
    }
}
