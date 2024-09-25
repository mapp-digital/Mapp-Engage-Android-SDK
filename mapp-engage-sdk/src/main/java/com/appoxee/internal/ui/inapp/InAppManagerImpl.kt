package com.appoxee.internal.ui.inapp

import android.app.Activity
import com.appoxee.internal.container.StatsContainer
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.NativeInappMessage
import com.appoxee.internal.model.response.inapp.TrackingParams
import com.appoxee.internal.model.response.inapp.WebInappMessage
import com.appoxee.internal.ui.inapp.nativ.NativeFactory
import com.appoxee.internal.ui.inapp.web.WebFactory
import com.appoxee.internal.util.Dispatchers
import com.appoxee.internal.util.DispatchersImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Class is responsible for taking all messages (native and web) into single list of messages.
 * After that it uses webFactory or nativeFactory to show inapp message one-by-one, ordered by templateId
 */
internal class InAppManagerImpl(
    private val nativeFactory: NativeFactory,
    private val webFactory: WebFactory,
    private val statsContainer: StatsContainer,
    private val scope: CoroutineScope,
    private val dispatchers: Dispatchers = DispatchersImpl(),
) : InAppManager {

    override fun parseResponse(response: InappResponse?): List<Message> {
        return response?.let {
            val messages = mutableListOf<Message>()
            messages.addAll(it.webMessages)
            messages.addAll(it.nativeMessages)
            messages.sortedBy { it.templateId }
        } ?: emptyList()
    }

    override fun handleMessages(activity: Activity, messages: List<Message>) {
        if (messages.isEmpty()) return
        val mutableMessages = messages.toMutableList()

        val message = messages.first()
        mutableMessages.removeFirst()
        showMessage(activity, message,
            onShow = { msg ->
                scope.launch {
                    reportInappEvent(msg, TrackingKey.IA_MSG_DISPLAYED, TrackingParams())
                }
            },
            onMessageClosed = { msg, key, params ->
                scope.launch {
                    reportInappEvent(msg, key, params)
                    withContext(dispatchers.mainDispatcher) {
                        if (mutableMessages.isNotEmpty())
                            handleMessages(activity, mutableMessages)
                    }
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
        statsContainer.statsClient.reportInappEvent(
            message.originalEventId, message.templateId, trackingKey,
            trackingParams.toMap()
        )
    }

}