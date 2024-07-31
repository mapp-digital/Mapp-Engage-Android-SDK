package com.appoxee.internal.push.base

import android.app.Notification
import com.appoxee.internal.push.model.PushData
import com.google.firebase.messaging.RemoteMessage

internal interface PushManager {
    fun handlePushMessage(remoteMessage: RemoteMessage)

    fun isPushMessageFromMapp(remoteMessage: RemoteMessage): Boolean

    suspend fun createNotification(pushData: PushData, notificationId: Int): Notification

    fun createNotificationChannel()

    fun showNotification(notification: Notification, notificationId: Int)

    fun dismissNotification(notificationId: Int)
}