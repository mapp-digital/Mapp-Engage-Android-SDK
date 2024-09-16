package com.appoxee.internal.model.response.inapp

import com.appoxee.internal.util.getNullableString
import com.appoxee.internal.util.getStringOrEmpty
import org.json.JSONObject

data class InappButton(
    val text: String,
    val textColor: String?,
    val backgroundColor: String?,
    val action: InappActionType?,
    val link: String?,
    val openInApp: Boolean
) {
    val actionData: ActionData
        get() = ActionData(link = link, openInApp = openInApp, actionType = action)

    companion object {
        fun fromJSON(json: JSONObject): InappButton {
            return InappButton(
                text = json.getStringOrEmpty("text"),
                textColor = json.getNullableString("text_color"),
                backgroundColor = json.getNullableString("background_color"),
                action = InappActionType.from(json.getNullableString("action")),
                link = json.getNullableString("link"),
                openInApp = json.getBoolean("open_inApp")
            )
        }
    }
}
