package com.appoxee.internal.push.base

import com.appoxee.internal.container.PushContainer
import com.appoxee.internal.util.Logger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MappMessagingService : FirebaseMessagingService() {

    private val TAG = MappMessagingService::class.java.name

    private lateinit var pushContainer: PushContainer

    override fun onCreate() {
        Logger.d(TAG, "MappMessagingService - onCreate()")
        super.onCreate()
        pushContainer = PushContainer(this)
        instance = this
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Logger.d(TAG, "MappMessagingService - onNewToken()")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Logger.d(TAG, "MappMessagingService - onMessageReceived()")
        pushContainer.pushManager.handlePushMessage(remoteMessage = message)
    }

    override fun onDestroy() {
        Logger.d(TAG, "MappMessagingService - onDestroy()")
        instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        @JvmStatic
        var instance: MappMessagingService? = null
            get
            set
    }
}