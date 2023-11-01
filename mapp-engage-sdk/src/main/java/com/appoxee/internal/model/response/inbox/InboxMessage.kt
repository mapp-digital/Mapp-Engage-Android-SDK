package com.appoxee.internal.model.response.inbox

import com.appoxee.internal.util.arrayToMap
import com.appoxee.internal.util.getLongOrDefault
import com.appoxee.internal.util.getNullableLong
import com.appoxee.internal.util.getNullableString
import com.appoxee.internal.util.getStringOrEmpty
import com.appoxee.internal.util.toMap
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


    companion object {
        fun fromJSON(json: JSONObject): InboxMessage {
            return InboxMessage(
                templateId = json.getLongOrDefault("template_id", 0),
                content = json.getStringOrEmpty("content"),
                subject = json.getStringOrEmpty("subject"),
                summary = json.getNullableString("summary"),
                iconUrl = json.getNullableString("icon_url"),
                sentDate = json.getNullableLong("sent_ts"),
                expireDate = json.getNullableLong("expire_ts"),
                firstSentTs = json.getNullableLong("firts_sent_ts"),
                status = MessageStatus.fromName(json.getStringOrEmpty("status")),
                isNativeInApp = json.getBoolean("is_native_in_app"),
                extras = json.arrayToMap("extras") { it.toString() }
            )
        }
    }
}
