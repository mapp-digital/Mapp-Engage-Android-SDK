package com.appoxee.internal.container

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.appoxee.internal.provider.IconProvider
import com.appoxee.internal.provider.IconProviderImpl
import com.appoxee.internal.provider.PendingIntentProvider
import com.appoxee.internal.provider.PendingIntentProviderImpl
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
    private val defaultChannelId: String = "${context.packageName}_CHANNEL_ID",
    private val legacyChannelId: String = "channel_id_001",
) {
    private val NOTIFICATION_CHANNEL_NAME = "${context.packageName} notification channel"

    private val dispatchersProvider: DispatchersProvider = DispatchersProviderImpl()

    private val notificationManager by lazy { NotificationManagerCompat.from(context) }

    private val notificationChannelId by lazy {
        resolveNotificationChannelId(notificationManager, defaultChannelId, legacyChannelId)
    }

    private val notify: Notify by lazy { NotifyImpl(context, notificationManager) }

    private val categoriesFactory: CategoriesFactory
        get() = CategoriesFactory(storage = appoxeeContainer.storage)

    private val notificationStyleFactory: NotificationStyleFactory
        get() = NotificationStyleFactory()

    private val iconProvider: IconProvider
        get() = IconProviderImpl(context)

    internal val pendingIntentProvider: PendingIntentProvider
        get() = PendingIntentProviderImpl(context)

    private val notificationBuilder: NotificationCompat.Builder
        get() = NotificationCompat.Builder(context, notificationChannelId)

    private val notificationFactory: NotificationFactory
        get() = NotificationFactory(
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
            notificationChannelId,
            NOTIFICATION_CHANNEL_NAME
        ).also {
            it.createNotificationChannel()
        }
}

internal fun resolveNotificationChannelId(
    notificationManager: NotificationManagerCompat,
    defaultChannelId: String,
    legacyChannelId: String,
): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return defaultChannelId

    // Keep the current v7 channel even if it is blocked or less important than the legacy one.
    if (notificationManager.getNotificationChannel(defaultChannelId) != null) return defaultChannelId
    if (notificationManager.getNotificationChannel(legacyChannelId) != null) return legacyChannelId
    return defaultChannelId
}
