package com.appoxee.internal.stats

import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.inbox.InboxMessageDto
import com.appoxee.internal.model.response.inbox.MessageStatusDto
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.util.DispatchersProvider
import com.appoxee.internal.util.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class StatsClientImpl(
    private val engageApi: EngageApi,
    private val dispatchersProvider: DispatchersProvider,
    private val maxDrainAttempts: Int = 3,
    private val maxBufferSize: Int = 100,
) : StatsClient {

    private val TAG = StatsClientImpl::class.java.name

    private data class BufferedEvent(
        val originalEventId: String,
        val templateId: Long,
        val trackingKey: TrackingKey,
        val trackingAttributes: Map<String, *>,
        val drainCount: Int = 0,
    )

    private val bufferMutex = Mutex()
    private val eventBuffer = ArrayDeque<BufferedEvent>()

    override suspend fun reportPushEvent(
        messageId: Long,
        sendoutId: Long,
        clickType: ClickType,
        eventType: EventType
    ) {
        withContext(dispatchersProvider.ioDispatcher) {
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
        withContext(dispatchersProvider.ioDispatcher) {
            // First attempt
            var response = engageApi.inappEvent(originalEventId, templateId, trackingKey, trackingAttributes)
            if (response.isSuccess()) {
                Logger.d(TAG, "InApp Event sent: $originalEventId, ${trackingKey.key}")
                drainBuffer()
                return@withContext
            }

            // Immediate retry for transient glitches
            response = engageApi.inappEvent(originalEventId, templateId, trackingKey, trackingAttributes)
            if (response.isSuccess()) {
                Logger.d(TAG, "InApp Event sent on retry: $originalEventId, ${trackingKey.key}")
                drainBuffer()
                return@withContext
            }

            // Both attempts failed — buffer for later drain cycles
            bufferMutex.withLock {
                if (eventBuffer.size >= maxBufferSize) eventBuffer.removeFirst()
                eventBuffer.addLast(
                    BufferedEvent(originalEventId, templateId, trackingKey, trackingAttributes, drainCount = 0)
                )
            }
            Logger.d(TAG, "InApp Event buffered for later: $originalEventId, ${trackingKey.key}")
        }
    }

    private suspend fun drainBuffer() {
        // Snapshot events buffered before this cycle — re-buffered events wait for the next cycle
        val snapshot = bufferMutex.withLock {
            val batch = eventBuffer.toList()
            eventBuffer.clear()
            batch
        }
        if (snapshot.isEmpty()) return

        for (event in snapshot) {
            val nextDrainCount = event.drainCount + 1
            if (nextDrainCount > maxDrainAttempts) {
                Logger.d(TAG, "InApp Event dropped after max retries: ${event.originalEventId}, ${event.trackingKey.key}")
                continue
            }
            val response = engageApi.inappEvent(
                event.originalEventId, event.templateId, event.trackingKey, event.trackingAttributes
            )
            if (response.isSuccess()) {
                Logger.d(TAG, "Buffered InApp Event sent (drain $nextDrainCount): ${event.originalEventId}, ${event.trackingKey.key}")
            } else {
                bufferMutex.withLock {
                    if (eventBuffer.size >= maxBufferSize) eventBuffer.removeFirst()
                    eventBuffer.addLast(event.copy(drainCount = nextDrainCount))
                }
            }
        }
    }

    override suspend fun reportActivation(seconds: Int) {
        withContext(dispatchersProvider.ioDispatcher) {
            val response = engageApi.activate(seconds.toLong())
            if (response.isSuccess()) {
                Logger.d(TAG, "Application was active: $seconds seconds")
            } else {
                Logger.e(TAG, "Error sending activation event: ${response.error?.message}")
            }
        }
    }

    override suspend fun markInboxMessageStatus(
        message: InboxMessageDto,
        status: MessageStatusDto
    ): Boolean = withContext(dispatchersProvider.ioDispatcher) {
        val originalEventId = message.eventId
        val templateId = message.templateId
        val trackingKey = status.toTrackingKey()
        val trackingAttributes = emptyMap<String, Any>()

        val response =
            engageApi.inappEvent(originalEventId, templateId, trackingKey, trackingAttributes)
        if (response.isSuccess()) {
            Logger.d(
                TAG,
                "Inbox message status updated successfully: $originalEventId, $templateId, ${trackingKey.key}"
            )
            true
        } else {
            Logger.e(TAG, "Inbox message status updated error: ${response.error?.message}")
            false
        }
    }
}
