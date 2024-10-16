package com.appoxee.internal.provider

import android.app.PendingIntent
import android.content.Intent
import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.push.model.PushData
import com.appoxee.internal.push.model.PushUriType


internal interface PendingIntentProvider {
    fun createPendingIntent(pushData: PushData, notificationId: Int, action: String?): PendingIntent?
    fun createDismissPendingIntent(notificationId: Int, pushData: PushData?): PendingIntent
    fun createCustomPendingIntent(
        uriType: PushUriType?,
        actionData: String?,
        action: String?,
        pushData: PushData?,
        notificationId: Int,
        eventType: EventType,
    ): PendingIntent

    fun createDelegateIntent(
        clickType: ClickType,
        eventType: EventType,
        notificationId: Int,
        action: String?,
        pushData: PushData?,
    ): Intent
}