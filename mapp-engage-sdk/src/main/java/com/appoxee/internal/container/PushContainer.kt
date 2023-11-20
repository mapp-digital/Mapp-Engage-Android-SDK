package com.appoxee.internal.container

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.appoxee.internal.push.NotificationFactory
import com.appoxee.internal.push.PushManager
import com.appoxee.internal.push.PushManagerImpl
import com.appoxee.shared.AppoxeeOptions

internal class PushContainer(private val context: Context) {
    internal lateinit var options: AppoxeeOptions

    private val NOTIFICATION_CHANNEL_NAME = "${context.packageName} notification channel"
    private val NOTIFICATION_CHANNEL_ID = "${context.packageName}_CHANNEL_ID"

    private val notificationManager by lazy { NotificationManagerCompat.from(context) }

    private val notificationFactory: NotificationFactory by lazy {
        NotificationFactory(
            context, NOTIFICATION_CHANNEL_ID
        )
    }

    internal val pushManager: PushManager by lazy {
        PushManagerImpl(
            notificationManager,
            notificationFactory,
            options,
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME
        ).also {
            it.createNotificationChannel()
        }
    }
}