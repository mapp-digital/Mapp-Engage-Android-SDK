package com.appoxee.internal.push

import com.appoxee.Appoxee
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MappMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        Appoxee.engage(this)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Appoxee.instance().optIn(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
    }
}