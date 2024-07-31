package com.appoxee.internal.provider

import android.app.PendingIntent
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.push.model.PushData
import com.appoxee.internal.push.model.PushUriType


internal interface PendingIntentProvider {
    fun createPendingIntent(pushData: PushData): PendingIntent?
    fun createDismissPendingIntent(notificationId: Int, pushData: PushData?): PendingIntent
    fun createCustomPendingIntent(
        uriType: PushUriType,
        actionData: String?,
        pushData: PushData?,
        notificationId: Int,
        eventType: EventType
    ): PendingIntent
}