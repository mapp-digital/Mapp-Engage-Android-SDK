package com.appoxee.internal.stats

import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class StatsClientImpl(
    private val engageApi: EngageApi,
    private val scope: CoroutineScope
) : StatsClient {
    private val TAG = StatsClientImpl::class.java.name
    override fun reportPushEvent(
        messageId: Long,
        sendoutId: Long,
        clickType: ClickType,
        eventType: EventType
    ) {
        scope.launch(Dispatchers.IO) {
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