package com.appoxee.internal.broadcast

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.container.StatsContainer
import com.appoxee.internal.container.StorageContainer
import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.push.model.PushData
import com.appoxee.internal.util.CompatExt.getParcelableCompat
import com.appoxee.internal.util.Logger
import org.jetbrains.annotations.TestOnly
import java.util.Objects

class MappInternalBroadcastReceiver : BroadcastReceiver() {
    private val TAG = MappInternalBroadcastReceiver::class.java.name
    private lateinit var statsContainer: StatsContainer

    override fun onReceive(context: Context?, intent: Intent?) {
        Logger.d(TAG, "onReceive: ${intent?.action}")

        val action = intent?.action?.let { ClickType.fromString(it) } ?: return

        context?.let {
            if (!::statsContainer.isInitialized) {
                statsContainer = StatsContainer(it)
            }

            if (Objects.equals(action, ClickType.DISMISS)) {
                intent.extras?.let { bundle ->
                    bundle.getInt("notificationId").let { notificationId ->
                        val notificationManager =
                            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                        notificationManager?.cancel(notificationId)
                    }
                    sendReportEvent(context, bundle)
                }
            }
        }
    }

    private fun sendReportEvent(context: Context?, bundle: Bundle?) {
        Logger.d(TAG, "sendReportEvent()")
        bundle?.getParcelableCompat<PushData>("pushData")?.let { pushData ->
            val messageId = pushData.id
            val sendoutId = pushData.sendoutId
            Logger.d(TAG, "sendReportEvent() - $messageId - $sendoutId")
            context?.let {
                Logger.d(TAG, "reportPushEvent() - before")
                statsContainer.statsClient.reportPushEvent(
                    messageId,
                    sendoutId ?: 0L,
                    ClickType.DISMISS,
                    EventType.DISMISS
                )
                Logger.d(TAG, "reportPushEvent() - after")
            }
        }
    }

    @TestOnly
    internal fun setStatsContainer(statsContainer: StatsContainer) {
        this.statsContainer = statsContainer
    }
}