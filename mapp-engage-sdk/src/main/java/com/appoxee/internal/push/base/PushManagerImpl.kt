package com.appoxee.internal.push.base

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.appoxee.internal.network.exceptions.DeviceNotRegisteredException
import com.appoxee.internal.push.model.PushData
import com.appoxee.internal.push.model.PushData.Companion.toPushData
import com.appoxee.internal.storage.Storage
import com.appoxee.internal.util.Logger
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.NotificationMode
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

internal class PushManagerImpl(
    private val context: Context,
    private val scope: CoroutineScope,
    private val notificationManager: NotificationManagerCompat,
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

    override fun handlePushMessage(remoteMessage: RemoteMessage) {
        scope.launch {
            if(!isPushMessageFromMapp(remoteMessage)) return@launch

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
                    withContext(Dispatchers.Main) {
                        showNotification(context, notification, notificationId)
                    }
                }
            }
        }
    }

    override fun isPushMessageFromMapp(remoteMessage: RemoteMessage): Boolean {
        return try {
            val id = remoteMessage.data["p"]
            val category = remoteMessage.data["category"]
            val userId = remoteMessage.data["user_id"]
            val customerId = remoteMessage.data["customer_id"]
            id != null && category != null && userId != null && customerId != null
        } catch (e: Exception) {
            false
        }

//        return pushData.id != 0L && /* id == 'p' parameter */
//                pushData.category != null &&
//                pushData.userId != null &&
//                pushData.customerId != null
    }

    override suspend fun createNotification(pushData: PushData, notificationId: Int): Notification {
        return notificationFactory.createSimpleNotification(pushData, notificationId)
    }

    override fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    notificationChannelId,
                    notificationChannelName,
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            )
        }
    }

    override fun showNotification(
        context: Context,
        notification: Notification,
        notificationId: Int
    ) {
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

    override fun dismissNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
}