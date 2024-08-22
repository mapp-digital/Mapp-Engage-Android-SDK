package com.appoxee.internal.broadcast

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.appoxee.internal.container.StatsContainer
import com.appoxee.internal.container.StorageContainer
import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.push.model.PushData
import com.appoxee.internal.util.CompatExt.getParcelableCompat
import com.appoxee.internal.util.Logger
import com.appoxee.shared.LocalPushBroadcast
import com.appoxee.shared.MappPush
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly
import java.util.Objects
import java.util.concurrent.TimeUnit

class MappInternalBroadcastReceiver : BroadcastReceiver() {
    private val TAG = MappInternalBroadcastReceiver::class.java.name
    private lateinit var statsContainer: StatsContainer
    private lateinit var storageContainer: StorageContainer

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context?, i: Intent?) {
        val action = i?.action
        Logger.d(TAG, "onReceive: $action")

        context?.let {
            i?.extras?.let { bundle ->
                if (!::statsContainer.isInitialized) {
                    statsContainer = StatsContainer(it)
                }

                if (!::storageContainer.isInitialized) {
                    storageContainer =
                        StorageContainer.getInstance(context, TimeUnit.HOURS.toMillis(1))
                }

                bundle.getParcelableCompat<PushData>("pushData")?.let { pushData ->
                    val clickType = i.getStringExtra("clickType")?.let { ClickType.fromString(it) }
                        ?: ClickType.LAUNCH_APP
                    val notificationId = bundle.getInt("notificationId")
                    val eventType = bundle.getInt("eventType").let { EventType.entries[it] }

                    // event for push received is not sent to a backend; all others are sent
                    if (LocalPushBroadcast.actionsForReporting.contains(action)) {
                        sendReportEvent(context, pushData, clickType, eventType)
                    }

                    // notify client app about push event
                    notifyClientApp(context, pushData, action)

                    // if event is dismiss, then try to clear notification from system status bar
                    if (Objects.equals(clickType, ClickType.DISMISS)) {
                        if (notificationId != 0) {
                            val notificationManager =
                                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                            notificationManager?.cancel(notificationId)
                        }
                    }
                }
            }
        }
    }

    private fun notifyClientApp(
        context: Context?,
        pushData: PushData,
        action: String?,
    ) {
        Logger.d(TAG, "notifyClientApp() - PushData: $pushData - action: $action")
        // delegate to subscribed app event

        scope.launch {
            val clazz = storageContainer.storage.getBroadcastClass()
            Logger.d(TAG, "notifyClientApp() - Broadcast class: ${clazz?.simpleName}")
            val intent = Intent(context, clazz).apply {
                setPackage(context?.applicationContext?.packageName)
                setAction(action)
                putExtra("mappPush", MappPush(pushData))
            }
            context?.sendBroadcast(intent)
        }

    }

    private fun sendReportEvent(
        context: Context?,
        pushData: PushData?,
        clickType: ClickType,
        eventType: EventType,
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