package com.appoxee.internal.push

import android.app.NotificationChannel
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.RemoteMessage

internal interface PushManager {
    fun handlePushMessage(remoteMessage: RemoteMessage)

    fun isPushMessageFromMapp(remoteMessage: RemoteMessage): Boolean

    fun createNotification(): NotificationCompat

    fun createNotificationChannel(): NotificationChannel

    fun showNotification(notification: NotificationCompat)

    fun dismissNotification(notificationId: Int)
}