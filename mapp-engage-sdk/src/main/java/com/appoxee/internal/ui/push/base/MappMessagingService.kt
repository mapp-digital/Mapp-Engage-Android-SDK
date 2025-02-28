package com.appoxee.internal.ui.push.base

import android.annotation.SuppressLint
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.container.PushContainer
import com.appoxee.internal.util.Logger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MappMessagingService : FirebaseMessagingService() {

    private val TAG = MappMessagingService::class.java.name

    private lateinit var pushContainer: PushContainer
    private lateinit var appoxeeContainer: AppoxeeContainer

    private val scope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        Logger.d(TAG, "MappMessagingService - onCreate()")
        super.onCreate()
        appoxeeContainer = AppoxeeContainer.getInstance(this)
        pushContainer = PushContainer(this, appoxeeContainer)
        instance = this
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Logger.d(TAG, "MappMessagingService - onMessageReceived()")
        log(message)
        scope.launch {
            pushContainer.pushManager.handlePushMessage(
                context = applicationContext,
                remoteMessage = message
            )
        }
    }

    override fun onDestroy() {
        Logger.d(TAG, "MappMessagingService - onDestroy()")
        instance = null
        super.onDestroy()
    }

    private fun log(message: RemoteMessage) {
        val sb = StringBuilder()
        sb.append("\"messageId\": ").append(message.messageId).append("\n")
        sb.append("\"messageType\": ").append(message.messageType).append("\n")
        sb.append("\"priority\": ").append(message.priority).append("\n")
        sb.append("{").append("\n")
        for ((k, v) in message.data) {
            sb.append("\t").append("\"$k\"").append(" : ").append("\"$v\"").append("\n")
        }
        sb.append("}")
        Logger.i(TAG, "RemoteMessage.data: $sb")
    }

    companion object {
        @Volatile
        @JvmStatic
        @TestOnly
        var instance: MappMessagingService? = null
            private set
    }
}