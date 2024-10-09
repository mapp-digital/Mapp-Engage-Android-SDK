package com.appoxee.internal.push.base

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.appoxee.internal.broadcast.MappInternalBroadcastReceiver
import com.appoxee.internal.network.exceptions.DeviceNotRegisteredException
import com.appoxee.internal.push.model.PushData
import com.appoxee.internal.push.model.PushData.Companion.toPushData
import com.appoxee.internal.storage.Storage
import com.appoxee.internal.util.Logger
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.LocalPushBroadcast
import com.appoxee.shared.NotificationMode
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.withContext
import kotlin.random.Random

internal class PushManagerImpl(
    private val dispatchers: com.appoxee.internal.util.Dispatchers,
    private val notify: Notify,
    private val notificationFactory: NotificationFactory,
    private val storage: Storage,
    private val notificationChannelId: String,
    private val notificationChannelName: String,
) : PushManager {

    private val TAG = PushManagerImpl::class.java.name
    private lateinit var options: AppoxeeOptions
    private suspend fun getOptions(): AppoxeeOptions {
        if (!::options.isInitialized) {
            options = storage.getInitOptions() ?: throw DeviceNotRegisteredException()
        }
        return options
    }

    private suspend fun getNotificationMode(): NotificationMode {
        return getOptions().notificationMode
    }

    override suspend fun handlePushMessage(context: Context, remoteMessage: RemoteMessage) {
        withContext(dispatchers.ioDispatcher) {
            if (!isPushMessageFromMapp(remoteMessage)) return@withContext

            val pushData = remoteMessage.toPushData()
            when (getNotificationMode()) {
                NotificationMode.SILENT_ONLY -> {
                    Logger.d(TAG, "SILENT ONLY $pushData")
                }

                NotificationMode.BACKGROUND_ONLY -> {
                    Logger.d(TAG, "BACKGROUND ONLY $pushData")
                }

                else -> {
                    Logger.d(TAG, "BACKGROUND AND FOREGROUND $pushData")
                    val notificationId = Random.nextInt(1, 100_000)
                    val notification = createNotification(pushData, notificationId)
                    withContext(dispatchers.mainDispatcher) {
                        showNotification(notification, notificationId)
                    }
                    reportPushReceived(context, pushData, LocalPushBroadcast.PUSH_RECEIVED)
                }
            }
        }
    }

    override fun isPushMessageFromMapp(remoteMessage: RemoteMessage): Boolean {
        return try {
            !remoteMessage.data["p"].isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun createNotification(pushData: PushData, notificationId: Int): Notification {
        return withContext(dispatchers.ioDispatcher) {
            notificationFactory.createSimpleNotification(
                pushData,
                notificationId
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
        notification: Notification,
        notificationId: Int
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