package com.appoxee.internal.ui.push.base

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import com.appoxee.internal.broadcast.MappInternalBroadcastReceiver
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.network.exceptions.DeviceNotRegisteredException
import com.appoxee.internal.ui.push.model.CategoriesFactory
import com.appoxee.internal.ui.push.model.PushData
import com.appoxee.internal.ui.push.model.PushData.Companion.toPushData
import com.appoxee.internal.ui.push.model.SilentType
import com.appoxee.internal.util.Logger
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.LocalPushBroadcast
import com.appoxee.shared.NotificationMode
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Keep
internal class PushManagerImpl(
    private val dispatchersProvider: com.appoxee.internal.util.DispatchersProvider,
    private val notify: Notify,
    private val notificationFactory: NotificationFactory,
    private val appoxeeContainer: AppoxeeContainer,
    private val categoriesFactory: CategoriesFactory,
    private val notificationChannelId: String,
    private val notificationChannelName: String,
) : PushManager {

    private val TAG = PushManagerImpl::class.java.name
    private lateinit var options: AppoxeeOptions
    private suspend fun getOptions(): AppoxeeOptions {
        if (!::options.isInitialized) {
            options =
                appoxeeContainer.storage.getInitOptions() ?: throw DeviceNotRegisteredException()
        }
        return options
    }

    private suspend fun getNotificationMode(): NotificationMode {
        return getOptions().notificationMode
    }

    override suspend fun handlePushMessage(context: Context, remoteMessage: RemoteMessage) {
        withContext(dispatchersProvider.defaultDispatcher) {
            if (!isPushMessageFromMapp(remoteMessage)) return@withContext
            val notificationMode = getNotificationMode()
            Logger.d(TAG, "NOTIFICATION MODE: $notificationMode")
            val pushData = remoteMessage.toPushData(categoriesFactory.getCategories())
            if (pushData.contentAvailable) {
                // silent push
                handleSilentPush(pushData)
                reportPushReceived(context, pushData, LocalPushBroadcast.PUSH_SILENT)
            } else {
                // regular push
                if (shouldShowNotification(
                        notificationMode, appoxeeContainer.activityLifecycleHandler.isInForeground()
                    )
                ) {
                    createAndShowNotification(pushData)
                } else {
                    Logger.i(
                        TAG,
                        "Application is in a foreground and notification will not be displayed!"
                    )
                }
                reportPushReceived(context, pushData, LocalPushBroadcast.PUSH_RECEIVED)
            }
        }
    }

    private fun shouldShowNotification(
        notificationMode: NotificationMode, isInForeground: Boolean
    ): Boolean {
        return when (notificationMode) {
            NotificationMode.SILENT_ONLY -> false
            NotificationMode.BACKGROUND_ONLY -> !isInForeground
            NotificationMode.BACKGROUND_AND_FOREGROUND -> true
        }
    }

    private suspend fun createAndShowNotification(pushData: PushData) {
        val notificationId = (System.currentTimeMillis() / 100).toInt()
        Logger.d(
            TAG, "BACKGROUND AND FOREGROUND $pushData - notificationId: $notificationId"
        )
        val notification = createNotification(pushData, notificationId)
        withContext(dispatchersProvider.mainDispatcher) {
            showNotification(notification, notificationId)
        }
    }

    override fun isPushMessageFromMapp(remoteMessage: RemoteMessage): Boolean {
        return try {
            !remoteMessage.data["p"].isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun handleSilentPush(pushData: PushData) {
        if (SilentType.SYS_OPТ_IN.value == pushData.silentType) {
            val isOptedIn = pushData.silentData.toBoolean()
            val token = FirebaseMessaging.getInstance().token.await()
            if (isOptedIn) appoxeeContainer.appoxeeAdapter.optIn(token)
            else appoxeeContainer.appoxeeAdapter.optOut(token)
        } else if (SilentType.SYS_SET_ALIAS.value == pushData.silentType) {
            val alias = pushData.silentData
            if (!alias.isNullOrBlank()) appoxeeContainer.appoxeeAdapter.setAlias(alias)
        }
    }

    override suspend fun createNotification(pushData: PushData, notificationId: Int): Notification {
        return withContext(dispatchersProvider.ioDispatcher) {
            notificationFactory.createSimpleNotification(
                pushData, notificationId
            )
        }
    }

    @SuppressLint("InlinedApi")
    override fun createNotificationChannel() {
        notify.createChannel(
            notificationChannelId,
            notificationChannelName,
            NotificationManager.IMPORTANCE_DEFAULT,
        )
    }

    override fun showNotification(
        notification: Notification, notificationId: Int
    ) {
        notify.showNotification(notification, notificationId)
    }

    override fun dismissNotification(notificationId: Int) {
        notify.closeNotification(notificationId)
    }

    override fun reportPushReceived(context: Context, pushData: PushData, action: String) {
        val intent = Intent(context, MappInternalBroadcastReceiver::class.java).apply {
            setAction(action)
            putExtra("pushData", pushData)
        }
        context.sendBroadcast(intent)
    }
}
