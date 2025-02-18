package com.appoxee.internal.ui.push.base

import android.app.Notification
import android.content.Context
import com.appoxee.internal.ui.push.model.PushData
import com.google.firebase.messaging.RemoteMessage

internal interface PushManager {
    suspend fun handlePushMessage(context: Context, remoteMessage: RemoteMessage)

    fun isPushMessageFromMapp(remoteMessage: RemoteMessage): Boolean

    suspend fun handleSilentPush(pushData: PushData)

    suspend fun createNotification(pushData: PushData, notificationId: Int): Notification

    fun createNotificationChannel()

    fun showNotification(notification: Notification, notificationId: Int)

    fun dismissNotification(notificationId: Int)

    fun reportPushReceived(context: Context, pushData: PushData, action: String)
}