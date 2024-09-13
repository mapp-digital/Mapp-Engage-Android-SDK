package com.appoxee.internal.stats

import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.model.request.events.ClickType

internal interface StatsClient {
    suspend fun reportPushEvent(
        messageId: Long,
        sendoutId: Long,
        clickType: ClickType,
        eventType: EventType
    )
}