package com.appoxee.internal.stats

import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.inbox.InboxMessageDto
import com.appoxee.internal.model.response.inbox.MessageStatusDto

internal interface StatsClient {
    suspend fun reportPushEvent(
        messageId: Long,
        sendoutId: Long,
        clickType: ClickType,
        eventType: EventType
    )

    suspend fun reportInappEvent(
        originalEventId: String,
        templateId: Long,
        trackingKey: TrackingKey,
        trackingAttributes: Map<String, *> = emptyMap<String, Any>()
    )

    suspend fun reportActivation(seconds: Int)

    suspend fun markInboxMessageStatus(message: InboxMessageDto, status: MessageStatusDto): Boolean
}