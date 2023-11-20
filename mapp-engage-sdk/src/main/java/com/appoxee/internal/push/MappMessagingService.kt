package com.appoxee.internal.push

import com.appoxee.Appoxee
import com.appoxee.internal.util.Logger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MappMessagingService : FirebaseMessagingService() {

    private val TAG=MappMessagingService::class.java.name

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Logger.d(TAG, "onNewToken()")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Logger.d(TAG, "onMessageReceived()")
        Appoxee.instance().handlePushMessage(message)
    }
}