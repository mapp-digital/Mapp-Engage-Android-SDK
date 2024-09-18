package com.appoxee.internal.ui.inapp

import android.app.Activity
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.TrackingParams

internal interface InAppManager {
    fun parseResponse(response: InappResponse?): List<Message>
    fun handleMessages(activity: Activity, messages: List<Message>)
    fun <T : Message> showMessage(
        activity: Activity,
        message: T,
        onShow: ((T) -> Unit)? = null,
        onMessageClosed: ((T, TrackingKey, TrackingParams) -> Unit)? = null
    )
    suspend fun reportInappEvent(
        message: Message,
        trackingKey: TrackingKey,
        trackingParams: TrackingParams,
    )
}