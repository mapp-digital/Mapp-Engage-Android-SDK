package com.appoxee.internal.ui.inapp

import android.app.Activity
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inapp.Message

internal interface InAppManager {
    fun parseResponse(response: InappResponse?): List<Message>
    fun handleMessages(activity: Activity, messages: List<Message>)
    fun <T : Message> showMessage(
        activity: Activity,
        message: T,
        onMessageClosed: ((T) -> Unit)? = null
    )
}