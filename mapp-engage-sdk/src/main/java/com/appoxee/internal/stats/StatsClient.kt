package com.appoxee.internal.stats

import com.appoxee.internal.model.request.events.PushAction
import com.appoxee.internal.model.request.events.NotificationClick

internal interface StatsClient {
    suspend fun reportPushEvent(
        messageId: Long,
        sendoutId: Long,
        pushAction: PushAction,
        eventType: NotificationClick
    )
}