package com.appoxee.internal.model.response.inapp

import com.appoxee.internal.util.getNullableString
import com.appoxee.internal.util.getStringOrEmpty
import org.json.JSONObject

data class InappButton(
    val text: String,
    val textColor: String?,
    val backgroundColor: String?,
    val action: String?,
    val link: String?,
    val openInApp: Boolean
) {
    companion object {
        fun fromJSON(json: JSONObject): InappButton {
            return InappButton(
                text = json.getStringOrEmpty("text"),
                textColor = json.getNullableString("textColor"),
                backgroundColor = json.getNullableString("backgroundColor"),
                action = json.getNullableString("action"),
                link = json.getNullableString("link"),
                openInApp = json.getBoolean("open_inApp")
            )
        }
    }
}
