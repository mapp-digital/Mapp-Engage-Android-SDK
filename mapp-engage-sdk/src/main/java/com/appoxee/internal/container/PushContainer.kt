package com.appoxee.internal.container

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.appoxee.internal.provider.IconProvider
import com.appoxee.internal.provider.IconProviderImpl
import com.appoxee.internal.provider.PendingIntentProvider
import com.appoxee.internal.provider.PendingIntentProviderImpl
import com.appoxee.internal.ui.push.base.NotificationBuilder
import com.appoxee.internal.ui.push.base.NotificationBuilderImpl
import com.appoxee.internal.ui.push.base.NotificationFactory
import com.appoxee.internal.ui.push.base.Notify
import com.appoxee.internal.ui.push.base.NotifyImpl
import com.appoxee.internal.ui.push.base.PushManager
import com.appoxee.internal.ui.push.base.PushManagerImpl
import com.appoxee.internal.ui.push.model.CategoriesFactory
import com.appoxee.internal.ui.push.style.NotificationStyleFactory
import com.appoxee.internal.util.DispatchersProvider
import com.appoxee.internal.util.DispatchersProviderImpl

internal class PushContainer(
    private val context: Context,
    private val appoxeeContainer: AppoxeeContainer,
) {
    private val NOTIFICATION_CHANNEL_NAME = "${context.packageName} notification channel"
    private val NOTIFICATION_CHANNEL_ID = "${context.packageName}_CHANNEL_ID"

    private val dispatchersProvider: DispatchersProvider = DispatchersProviderImpl()

    private val notificationManager by lazy { NotificationManagerCompat.from(context) }

    private val notify: Notify by lazy { NotifyImpl(context, notificationManager) }

    private val categoriesFactory: CategoriesFactory
        get() = CategoriesFactory(storage = appoxeeContainer.storage)

    private val notificationStyleFactory: NotificationStyleFactory
        get() = NotificationStyleFactory()

    private val iconProvider: IconProvider
        get() = IconProviderImpl(context)

    internal val pendingIntentProvider: PendingIntentProvider
        get() = PendingIntentProviderImpl(context)

    private val notificationBuilder: NotificationBuilder
        get() = NotificationBuilderImpl(
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        )

    private val notificationFactory: NotificationFactory
        get() = NotificationFactory(
            categoriesFactory,
            notificationStyleFactory,
            notificationBuilder,
            iconProvider,
            pendingIntentProvider
        )

    internal val pushManager: PushManager
        get() = PushManagerImpl(
            dispatchersProvider,
            notify,
            notificationFactory,
            appoxeeContainer,
            categoriesFactory,
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME
        ).also {
            it.createNotificationChannel()
        }
}