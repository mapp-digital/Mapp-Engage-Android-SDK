package com.appoxee.internal.container

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.appoxee.internal.push.base.NotificationFactory
import com.appoxee.internal.push.base.PushManager
import com.appoxee.internal.push.base.PushManagerImpl
import com.appoxee.internal.push.model.CategoriesFactory
import com.appoxee.internal.push.style.NotificationStyleFactory

internal class PushContainer(
    context: Context
) {
    private val NOTIFICATION_CHANNEL_NAME = "${context.packageName} notification channel"
    private val NOTIFICATION_CHANNEL_ID = "${context.packageName}_CHANNEL_ID"

    private val storageContainer: StorageContainer by lazy { StorageContainer.getInstance(context) }

    private val notificationManager by lazy { NotificationManagerCompat.from(context) }

    private val categoriesFactory by lazy { CategoriesFactory(storage = storageContainer.storage) }

    private val notificationStyleFactory: NotificationStyleFactory by lazy { NotificationStyleFactory() }

    private val notificationFactory: NotificationFactory by lazy {
        NotificationFactory(
            context, categoriesFactory, notificationStyleFactory, NOTIFICATION_CHANNEL_ID
        )
    }

    internal val pushManager: PushManager by lazy {
        PushManagerImpl(
            notificationManager,
            notificationFactory,
            storageContainer.storage,
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME
        ).also {
            it.createNotificationChannel()
        }
    }
}