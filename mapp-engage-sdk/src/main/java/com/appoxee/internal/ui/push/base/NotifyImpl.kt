package com.appoxee.internal.ui.push.base

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.appoxee.internal.util.Logger

class NotifyImpl(
    private val context: Context,
    private val notificationManager: NotificationManagerCompat
) : Notify {
    private val TAG = NotifyImpl::class.java.simpleName

    override fun showNotification(notification: Notification, notificationId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val postNotificationPermission =
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            if (postNotificationPermission != PackageManager.PERMISSION_GRANTED) {
                Logger.e(
                    TAG,
                    "Permission ${Manifest.permission.POST_NOTIFICATIONS} is not granted!!!"
                )
                return
            }
        }
        notificationManager.notify(notificationId, notification)
    }

    override fun closeNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    override fun createChannel(
        channelId: String,
        channelName: String,
        importance: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    channelName,
                    importance,
                )
            )
        }
    }
}