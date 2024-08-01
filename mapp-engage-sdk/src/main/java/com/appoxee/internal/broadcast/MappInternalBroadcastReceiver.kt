package com.appoxee.internal.broadcast

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.appoxee.internal.container.StatsContainer
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

            val bundle = intent.extras
            val pushData = bundle?.getParcelableCompat<PushData>("pushData")
            val notificationId = bundle?.getInt("notificationId")
            val eventType =
                bundle?.getInt("eventType")?.let { EventType.entries[it] } ?: EventType.CLICK

            sendReportEvent(context, pushData, action, eventType)

            if (Objects.equals(action, ClickType.DISMISS)) {
                notificationId?.let { id ->
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    notificationManager?.cancel(id)
                }
            }
        }
    }

    private fun sendReportEvent(
        context: Context?,
        pushData: PushData?,
        clickType: ClickType,
        eventType: EventType
    ) {
        Logger.d(TAG, "sendReportEvent() - Action: $clickType - Event: $eventType")
        val data = pushData ?: return
        val messageId = data.id
        val sendoutId = data.sendoutId
        context?.let {
            statsContainer.statsClient.reportPushEvent(
                messageId,
                sendoutId ?: 0L,
                clickType,
                eventType,
            )
        }
    }

    @TestOnly
    internal fun setStatsContainer(statsContainer: StatsContainer) {
        this.statsContainer = statsContainer
    }
}