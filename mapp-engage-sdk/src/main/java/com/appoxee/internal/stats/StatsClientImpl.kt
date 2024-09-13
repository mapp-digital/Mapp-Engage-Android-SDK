package com.appoxee.internal.stats

import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.util.Dispatchers
import com.appoxee.internal.util.Logger
import kotlinx.coroutines.withContext

internal class StatsClientImpl(
    private val engageApi: EngageApi,
    private val dispatchers: Dispatchers
) : StatsClient {
    private val TAG = StatsClientImpl::class.java.name
    override suspend fun reportPushEvent(
        messageId: Long,
        sendoutId: Long,
        clickType: ClickType,
        eventType: EventType
    ) {
        withContext(dispatchers.ioDispatcher) {
            val response = engageApi.pushEvent(messageId, sendoutId, clickType, eventType)
            if (response.isSuccess()) {
                Logger.d(
                    TAG,
                    "Push Event sent successfully: $messageId, $sendoutId, ${clickType.name}, ${eventType.name}"
                )
            } else {
                Logger.e(TAG, "Push Event sending error: ${response.error?.message}")
            }
        }
    }
}