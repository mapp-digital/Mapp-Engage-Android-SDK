package com.appoxee.internal.ui.inapp

import android.app.Activity
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.NativeInappMessage
import com.appoxee.internal.model.response.inapp.WebInappMessage
import com.appoxee.internal.ui.inapp.nativ.NativeTemplateFactory
import com.appoxee.internal.ui.inapp.web.WebTemplateFactory

internal class InAppManagerImpl(
    private val nativeTemplateFactory: NativeTemplateFactory,
    private val webTemplateFactory: WebTemplateFactory
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
                nativeTemplateFactory.createBanner(activity, message, onMessageClosed)
            }

            is WebInappMessage -> {
                webTemplateFactory.createBanner(activity, message)
            }
        }
    }

}