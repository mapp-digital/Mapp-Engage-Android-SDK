package com.appoxee.internal.push

import android.app.NotificationChannel
import android.content.Context
import androidx.core.app.NotificationCompat
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.NotificationMode
import com.google.firebase.messaging.RemoteMessage

class PushManagerImpl(context: Context, private val options: AppoxeeOptions) : PushManager {

    private val notificationMode: NotificationMode
        get() = options.notificationMode

    override fun handlePushMessage(remoteMessage: RemoteMessage) {
        if (notificationMode == NotificationMode.SILENT_ONLY) {

        } else if (notificationMode == NotificationMode.BACKGROUND_ONLY) {

        } else {
            val notification = createNotification()
            showNotification(notification)
        }
    }

    override fun isPushMessageFromMapp(remoteMessage: RemoteMessage): Boolean {
        TODO("Not yet implemented")
    }

    override fun createNotification(): NotificationCompat {
        TODO("Not yet implemented")
    }

    override fun createNotificationChannel(): NotificationChannel {
        TODO("Not yet implemented")
    }

    override fun showNotification(notification: NotificationCompat) {
        TODO("Not yet implemented")
    }

    override fun dismissNotification(notificationId: Int) {
        TODO("Not yet implemented")
    }
}