package com.appoxee.internal.model.response.inapp

import com.appoxee.internal.util.arrayToList
import com.appoxee.internal.util.getLongOrDefault
import com.appoxee.internal.util.getNullableString
import com.appoxee.internal.util.getStringOrEmpty
import org.json.JSONObject

data class NativeInappMessage(
    override val templateId: String,
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
) : Message(templateId, content, type, behaviour, location) {
    companion object {
        fun fromJSON(json: JSONObject): NativeInappMessage {
            return NativeInappMessage(
                templateId = json.getStringOrEmpty("template_id"),
                content = json.getStringOrEmpty("content"),
                type = InappType.from(json.getLongOrDefault("type", 0).toInt()),
                behaviour = json.getJSONObject("behaviour")?.let { Behaviour.fromJSON(it) },
                location = json.getJSONObject("location")?.let { Location.fromJSON(it) },
                imageUrl = json.getNullableString("imageURL"),
                title = json.getStringOrEmpty("title"),
                titleColor = json.getNullableString("title_color"),
                templateBackgroundColor = json.getNullableString("template_background_color"),
                contentColor = json.getNullableString("content_color"),
                buttons = json.arrayToList("buttons") { InappButton.fromJSON(it) },
                contentTemplateId = ContentTemplates.from(json.getStringOrEmpty("content_template_id"))
            )
        }
    }
}
