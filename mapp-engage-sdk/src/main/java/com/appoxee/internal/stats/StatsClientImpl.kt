package com.appoxee.internal.stats

import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.util.Dispatchers
import com.appoxee.internal.util.Logger
import kotlinx.coroutines.withContext

internal class StatsClientImpl(
    private val engageApi: EngageApi,
    private val dispatchers: Dispatchers,
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

    override suspend fun reportInappEvent(
        originalEventId: String,
        templateId: Long,
        trackingKey: TrackingKey,
        trackingAttributes: Map<String, *>
    ) {
        withContext(dispatchers.ioDispatcher) {
            val response =
                engageApi.inappEvent(originalEventId, templateId, trackingKey, trackingAttributes)
            if (response.isSuccess()) {
                Logger.d(
                    TAG,
                    "InApp Event sent successfully: $originalEventId, $templateId, ${trackingKey.key}, $trackingAttributes"
                )
            } else {
                Logger.e(TAG, "InApp Event sending error: ${response.error?.message}")
            }
        }
    }

    override suspend fun reportActivation(seconds: Int) {
        withContext(dispatchers.ioDispatcher) {
            val response = engageApi.activate(seconds.toLong())
            if (response.isSuccess()) {
                Logger.d(
                    TAG,
                    "Application was active: $seconds seconds"
                )
            } else {
                Logger.e(TAG, "Error sending activation event: ${response.error?.message}")
            }
        }
    }

}