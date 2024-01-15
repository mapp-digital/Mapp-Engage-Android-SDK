package com.appoxee.internal.push.base

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.appoxee.internal.push.model.PushData
import com.appoxee.internal.push.model.PushData.Companion.toPushData
import com.appoxee.internal.util.Logger
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.NotificationMode
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

internal class PushManagerImpl(
    private val notificationManager: NotificationManagerCompat,
    private val notificationFactory: NotificationFactory,
    private val options: AppoxeeOptions,
    private val notificationChannelId: String,
    private val notificationChannelName: String,
) : PushManager {

    private val TAG = PushManagerImpl::class.java.name

    private val scope = CoroutineScope(Dispatchers.Default)

    private val notificationMode: NotificationMode
        get() = options.notificationMode

    override fun handlePushMessage(remoteMessage: RemoteMessage){
        val pushData = remoteMessage.toPushData()

        when (notificationMode) {
            NotificationMode.SILENT_ONLY -> {
                Logger.d(TAG, "SILENT ONLY $pushData")
            }

            NotificationMode.BACKGROUND_ONLY -> {
                Logger.d(TAG, "BACKGROUND ONLY $pushData")
            }

            else -> {
                scope.launch {
                    Logger.d(TAG, "BACKGROUND AND FOREGROUND $pushData")
                    val notificationId = Random.nextInt(1, 100_000)
                    val notification = createNotification(pushData, notificationId)
                    showNotification(notification, notificationId)
                }
            }
        }
    }

    override fun isPushMessageFromMapp(pushData: PushData): Boolean {
        return pushData.id != 0L && pushData.category != null && pushData.userId != null && pushData.customerId != null
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

    override fun showNotification(notification: Notification, notificationId: Int) {
        notificationManager.notify(notificationId, notification)
    }

    override fun dismissNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
}