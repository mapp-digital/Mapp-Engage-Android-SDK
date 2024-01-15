package com.appoxee.internal.stats

import com.appoxee.internal.AppoxeeAdapter
import com.appoxee.internal.model.request.events.PushAction
import com.appoxee.internal.model.request.events.NotificationClick
import com.appoxee.internal.util.Logger

internal class StatsClientImpl(private val appoxeeAdapter: AppoxeeAdapter) : StatsClient {
    private val TAG = StatsClientImpl::class.java.name
    override suspend fun reportPushEvent(
        messageId: Long,
        sendoutId: Long,
        pushAction: PushAction,
        eventType: NotificationClick
    ) {
        val response = appoxeeAdapter.pushEvent(messageId, sendoutId, pushAction, eventType)
        if (response.isSuccess()) {
            Logger.d(
                TAG,
                "Push Event sent successfully: $messageId, $sendoutId, ${pushAction.name}, ${eventType.name}"
            )
        } else {
            Logger.e(TAG, "Push Event sending error: ${response.error?.message}")
        }
    }
}