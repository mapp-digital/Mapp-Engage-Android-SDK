package com.appoxee.internal.model.response.inapp

import com.appoxee.internal.util.arrayToList
import com.appoxee.internal.util.getLongOrDefault
import com.appoxee.internal.util.getNullableString
import com.appoxee.internal.util.getStringOrEmpty
import org.json.JSONObject

data class NativeInappMessage(
    val templateId: String,
    val content: String,
    val type: Int,
    val behaviour: Behaviour?,
    val location: Location?,
    val imageUrl: String?,
    val title: String,
    val titleColor: String?,
    val templateBackgroundColor: String?,
    val contentColor: String?,
    val buttons: List<InappButton>,
    val contentTemplateId: String
) {
    companion object {
        fun fromJSON(json: JSONObject): NativeInappMessage {
            return NativeInappMessage(
                templateId = json.getStringOrEmpty("template_id"),
                content = json.getStringOrEmpty("content"),
                type = json.getLongOrDefault("type", 0).toInt(),
                behaviour = json.getJSONObject("behaviour")?.let { Behaviour.fromJSON(it) },
                location = json.getJSONObject("location")?.let { Location.fromJSON(it) },
                imageUrl = json.getNullableString("imageURL"),
                title = json.getStringOrEmpty("title"),
                titleColor = json.getNullableString("title_color"),
                templateBackgroundColor = json.getNullableString("template_background_color"),
                contentColor = json.getNullableString("content_color"),
                buttons = json.arrayToList("buttons") { InappButton.fromJSON(it) },
                contentTemplateId = json.getStringOrEmpty("content_template_id")
            )
        }
    }
}
