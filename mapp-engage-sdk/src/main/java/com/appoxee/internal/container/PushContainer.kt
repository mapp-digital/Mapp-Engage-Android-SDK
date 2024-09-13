package com.appoxee.internal.container

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.appoxee.internal.provider.IconProvider
import com.appoxee.internal.provider.IconProviderImpl
import com.appoxee.internal.provider.PendingIntentProvider
import com.appoxee.internal.provider.PendingIntentProviderImpl
import com.appoxee.internal.push.base.NotificationBuilder
import com.appoxee.internal.push.base.NotificationBuilderImpl
import com.appoxee.internal.push.base.NotificationFactory
import com.appoxee.internal.push.base.Notify
import com.appoxee.internal.push.base.NotifyImpl
import com.appoxee.internal.push.base.PushManager
import com.appoxee.internal.push.base.PushManagerImpl
import com.appoxee.internal.push.model.CategoriesFactory
import com.appoxee.internal.push.style.NotificationStyleFactory
import com.appoxee.internal.util.Dispatchers
import com.appoxee.internal.util.DispatchersImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

internal class PushContainer(
    private val context: Context,
) {
    private val NOTIFICATION_CHANNEL_NAME = "${context.packageName} notification channel"
    private val NOTIFICATION_CHANNEL_ID = "${context.packageName}_CHANNEL_ID"

    private val dispatchers: Dispatchers = DispatchersImpl()

    private val storageContainer: StorageContainer by lazy { StorageContainer.getInstance(context) }

    private val notificationManager by lazy { NotificationManagerCompat.from(context) }

    private val categoriesFactory by lazy { CategoriesFactory(storage = storageContainer.storage) }

    private val notificationStyleFactory: NotificationStyleFactory by lazy { NotificationStyleFactory() }

    internal val iconProvider: IconProvider by lazy { IconProviderImpl(context) }

    internal val pendingIntentProvider: PendingIntentProvider by lazy {
        PendingIntentProviderImpl(
            context
        )
    }

    private val notificationBuilder: NotificationBuilder by lazy {
        NotificationBuilderImpl(
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        )
    }


    private val notificationFactory: NotificationFactory by lazy {
        NotificationFactory(
            categoriesFactory,
            notificationStyleFactory,
            notificationBuilder,
            iconProvider,
            pendingIntentProvider,
            dispatchers
        )
    }

    internal val notify: Notify by lazy { NotifyImpl(context, notificationManager) }

    internal val pushManager: PushManager by lazy {
        PushManagerImpl(
            dispatchers,
            notify,
            notificationFactory,
            storageContainer.storage,
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME
        ).also {
            it.createNotificationChannel()
        }
    }
}