package eu.brrm.shared_ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import com.appoxee.internal.ui.push.base.MappMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/**
 * Create local push messages and deliver them to the registered FirebaseMessagingService
 */
object LocalNotifications {

    const val messagingEvent = "com.google.firebase.MESSAGING_EVENT"
    fun createNotification(
        context: Context,
        messageAction: Map<String, String>?,
        media: Map<String, String>?,
        actionButtons: List<Map<String, String>>
    ) {
        val bundle = getBundle(messageAction, media, actionButtons)
        val connection = object : ServiceConnection {

            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                com.appoxee.internal.ui.push.base.MappMessagingService.instance?.onMessageReceived(RemoteMessage(bundle))
                context.unbindService(this)
            }

            override fun onServiceDisconnected(arg0: ComponentName) {

            }
        }

        context.bindService(Intent(messagingEvent).apply {
            setPackage(context.packageName)
            putExtras(bundle)
        }, connection, Context.BIND_AUTO_CREATE)

    }

    private fun getBundle(
        messageAction: Map<String, String>?,
        media: Map<String, String>?,
        actionButtons: List<Map<String, String>>
    ): Bundle {
        val random = Random(System.currentTimeMillis())
        return Bundle().apply {
            putString("p", random.nextLong(1, 10000).toString())
            putString("sendout_id", random.nextLong(1, 10000).toString())
            putString("push_title", "Local notification")
            putString("alert", "Testing notifications...")
            putString("big_text_body", "Big body text...")
            putString("category", "apx_read_open")
            putString("language", "en")
            putString("user_id",random.nextLong(1000,100_000).toString())
            putString("customer_id",random.nextLong(1000,100_000).toString())
            media?.entries?.first()?.let {
                putString("type", it.key)
                putString("ios_apx_media", it.value)
            }
            messageAction?.entries?.forEach {
                putString(it.key, it.value)
            }

            val buttons = JSONArray().apply {
                put(JSONObject().apply {
                    put("fgAction", JSONObject().also { json ->
                        actionButtons.map {
                            it.entries.forEach {
                                json.put(it.key, it.value)
                            }
                        }
                    })
                })
            }.toString()

            putString("buttons", buttons)
        }
    }
}