package com.appoxee.internal.model.response.inbox

import com.appoxee.internal.model.response.inapp.Behaviour
import com.appoxee.internal.model.response.inapp.InappType
import com.appoxee.internal.model.response.inapp.NativeInappMessage
import com.appoxee.internal.model.response.inapp.WebInappMessage
import com.appoxee.internal.util.LibraryExtensions.decode
import com.appoxee.internal.util.arrayToMap
import com.appoxee.internal.util.getLongOrDefault
import com.appoxee.internal.util.getNullableLong
import com.appoxee.internal.util.getNullableString
import com.appoxee.internal.util.getStringOrEmpty
import org.json.JSONObject

data class InboxMessage(
    val templateId: Long,
    val content: String,
    val subject: String,
    val summary: String?,
    val iconUrl: String?,
    val sentDate: Long?,
    val expireDate: Long?,
    val firstSentTs: Long?,
    val status: MessageStatus,
    val isNativeInApp: Boolean,
    val extras: Map<String, String>
) {

    private var nativeInappMessage: NativeInappMessage? = null
    private var webInappMessage: WebInappMessage? = null

    companion object {
        fun fromJSON(json: JSONObject, eventId: String, eventKey: String): InboxMessage {
            val isNativeInApp = json.optBoolean("is_native_in_app")
            val inboxMessage = InboxMessage(
                templateId = json.getLongOrDefault("template_id", 0),
                content = json.getStringOrEmpty("content").decode(),
                subject = json.getStringOrEmpty("subject"),
                summary = json.getNullableString("summary"),
                iconUrl = json.getNullableString("icon_url"),
                sentDate = json.getNullableLong("sent_ts"),
                expireDate = json.getNullableLong("expire_ts"),
                firstSentTs = json.getNullableLong("firts_sent_ts"),
                status = MessageStatus.fromName(json.getStringOrEmpty("status")),
                isNativeInApp = isNativeInApp,
                extras = json.arrayToMap("extras") { it.toString() }
            ).apply {
                if (isNativeInApp) {
                    nativeInappMessage =
                        NativeInappMessage.fromJSON(JSONObject(content), eventId, eventKey)
                } else {
                    webInappMessage = WebInappMessage(
                        eventId, eventKey, templateId, content, InappType.DIALOG,
                        Behaviour(0, 0), location = null,
                    )
                }
            }

            return inboxMessage
        }
    }
}
