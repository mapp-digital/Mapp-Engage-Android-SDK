package com.appoxee.shared

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
    fun setStatus(status: MessageStatus): InboxMessage {
        return this.copy(status = status)
    }

    override fun toString(): String {
        return "InboxMessage(templateId=$templateId, subject='$subject', summary=$summary, iconUrl=$iconUrl, sentDate=$sentDate, expireDate=$expireDate, firstSentTs=$firstSentTs, status=$status, isNativeInApp=$isNativeInApp, extras=$extras)"
    }
}
