package com.appoxee.internal.model.response.inbox

import com.appoxee.shared.InboxMessage
import com.appoxee.shared.InboxMessagesResponse
import com.appoxee.shared.MessageStatus

internal fun MessageStatusDto.toPublic(): MessageStatus = when (this) {
    MessageStatusDto.READ -> MessageStatus.READ
    MessageStatusDto.UNREAD -> MessageStatus.UNREAD
    MessageStatusDto.DELETED -> MessageStatus.DELETED
}

internal fun MessageStatus.toDto(): MessageStatusDto = when (this) {
    MessageStatus.READ -> MessageStatusDto.READ
    MessageStatus.UNREAD -> MessageStatusDto.UNREAD
    MessageStatus.DELETED -> MessageStatusDto.DELETED
}

internal fun InboxMessageDto.toPublic(): InboxMessage = InboxMessage(
    templateId = templateId,
    content = content,
    subject = subject,
    summary = summary,
    iconUrl = iconUrl,
    sentDate = sentDate,
    expireDate = expireDate,
    firstSentTs = firstSentTs,
    status = status.toPublic(),
    isNativeInApp = isNativeInApp,
    extras = extras,
    eventId = eventId,
    eventKey = eventKey,
)

internal fun com.appoxee.internal.model.response.inbox.InboxMessagesResponse.toPublic(): InboxMessagesResponse =
    InboxMessagesResponse(
        eventId = eventId,
        messages = messages.map { it.toPublic() }
    )
