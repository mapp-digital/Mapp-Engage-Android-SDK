package com.appoxee.internal.ui.inapp

import android.app.Activity
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.NativeInappMessage
import com.appoxee.internal.model.response.inapp.TrackingParams
import com.appoxee.internal.model.response.inapp.WebInappMessage
import com.appoxee.internal.model.response.inbox.InboxMessage
import com.appoxee.internal.model.response.inbox.MessageStatus
import com.appoxee.internal.stats.StatsClient
import com.appoxee.internal.ui.inapp.nativ.NativeFactory
import com.appoxee.internal.ui.inapp.web.WebFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Class is responsible for taking all messages (native and web) into single list of messages.
 * After that it uses webFactory or nativeFactory to show inapp message one-by-one, ordered by templateId
 */
internal class InAppManagerImpl(
    private val nativeFactory: NativeFactory,
    private val webFactory: WebFactory,
    private val statsClient: StatsClient,
    private val scope: CoroutineScope,
) : InAppManager {

    override fun parseResponse(response: InappResponse?): List<Message> {
        if (response == null) return emptyList()
        val messages = mutableListOf<Message>()
        messages.addAll(response.webMessages)
        messages.addAll(response.nativeMessages)
        return messages.sortedByDescending { it.templateId }
    }

    override fun handleMessages(activity: Activity, messages: List<Message>) {
        if (messages.isEmpty()) return

        val first = messages.first()
        val skipped = messages.drop(1)

        scope.launch {
            skipped.forEach { msg ->
                reportInappEvent(
                    msg,
                    TrackingKey.IA_MSG_NOT_DISPLAYED,
                    TrackingParams(reason = TrackingParams.REASON_OTHER_MESSAGE_DISPLAYING)
                )
            }
        }

        showMessage(activity, first,
            onShow = { msg ->
                scope.launch {
                    reportInappEvent(msg, TrackingKey.IA_MSG_DISPLAYED, TrackingParams())
                }
            },
            onMessageClosed = { msg, key, params ->
                scope.launch {
                    reportInappEvent(msg, key, params)
                }
            })
    }

    override fun <T : Message> showMessage(
        activity: Activity,
        message: T,
        onShow: ((T) -> Unit)?,
        onMessageClosed: ((T, TrackingKey, TrackingParams) -> Unit)?
    ) {
        when (message) {
            is NativeInappMessage -> {
                nativeFactory.createBanner(
                    activity,
                    message,
                    onShow = onShow,
                    onMessageClosed = onMessageClosed
                )
            }

            is WebInappMessage -> {
                webFactory.createBanner(activity, message, onShow, onMessageClosed)
            }
        }
    }

    override suspend fun reportInappEvent(
        message: Message,
        trackingKey: TrackingKey,
        trackingParams: TrackingParams,
    ) {
        statsClient.reportInappEvent(
            message.originalEventId, message.templateId, trackingKey,
            trackingParams.toMap()
        )
    }

    override suspend fun markInboxMessageStatus(
        message: InboxMessage,
        status: MessageStatus
    ): Boolean {
        return statsClient.markInboxMessageStatus(message, status)
    }

}