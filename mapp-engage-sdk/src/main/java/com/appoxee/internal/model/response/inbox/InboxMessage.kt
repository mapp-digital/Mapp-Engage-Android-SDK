package com.appoxee.internal.model.response.inbox

import com.appoxee.internal.model.response.inapp.BannerPosition
import com.appoxee.internal.model.response.inapp.Behaviour
import com.appoxee.internal.model.response.inapp.InappType
import com.appoxee.internal.model.response.inapp.Location
import com.appoxee.internal.model.response.inapp.Message
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
    val extras: Map<String, String>,
    val eventId: String,
    val eventKey: String,
) {
    fun <T : Message> getInappMessage(): T {
        return if (isNativeInApp) {
            NativeInappMessage.fromJSON(JSONObject(content), eventId, eventKey) as T
        } else {
            WebInappMessage(
                originalEventId = eventId,
                originalEventKey = eventKey,
                templateId = templateId,
                content = content,
                type = InappType.DIALOG,
                behaviour = Behaviour(0, 15),
                Location(BannerPosition.BOTTOM,60,80)
            ) as T
        }
    }

    fun setStatus(status: MessageStatus): InboxMessage {
        return this.copy(status = status)
    }

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
                extras = json.arrayToMap("extras") { it.toString() },
                eventId = eventId,
                eventKey = eventKey
            )

            return inboxMessage
        }
    }

    override fun toString(): String {
        return "InboxMessage(templateId=$templateId, subject='$subject', summary=$summary, iconUrl=$iconUrl, sentDate=$sentDate, expireDate=$expireDate, firstSentTs=$firstSentTs, status=$status, isNativeInApp=$isNativeInApp, extras=$extras)"
    }
}
