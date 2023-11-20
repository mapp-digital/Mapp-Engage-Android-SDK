package com.appoxee.internal.push

import android.app.Notification
import com.appoxee.internal.push.model.PushData
import com.google.firebase.messaging.RemoteMessage

internal interface PushManager {
    fun handlePushMessage(remoteMessage: RemoteMessage)

    fun isPushMessageFromMapp(pushData: PushData): Boolean

    fun createNotification(pushData: PushData): Notification

    fun createNotificationChannel()

    fun showNotification(notification: Notification)

    fun dismissNotification(notificationId: Int)
}