package com.appoxee.internal.ui.inapp

import android.app.Activity
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.NativeInappMessage
import com.appoxee.internal.model.response.inapp.WebInappMessage
import com.appoxee.internal.ui.inapp.nativ.NativeFactory
import com.appoxee.internal.ui.inapp.web.WebFactory

/**
 * Class is responsible for taking all messages (native and web) into single list of messages.
 * After that it uses webFactory or nativeFactory to show inapp message one-by-one, ordered by templateId
 */
internal class InAppManagerImpl(
    private val nativeFactory: NativeFactory,
    private val webFactory: WebFactory
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
        showMessage(activity, message) {
            if (mutableMessages.isNotEmpty()) handleMessages(activity, mutableMessages)
        }
    }

    override fun <T : Message> showMessage(
        activity: Activity, message: T, onMessageClosed: ((T) -> Unit)?
    ) {
        when (message) {
            is NativeInappMessage -> {
                nativeFactory.createBanner(activity, message, onMessageClosed)
            }

            is WebInappMessage -> {
                webFactory.createBanner(activity, message, onMessageClosed)
            }
        }
    }

}