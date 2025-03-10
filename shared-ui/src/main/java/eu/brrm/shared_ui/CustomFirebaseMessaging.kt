package eu.brrm.shared_ui

import com.appoxee.Appoxee
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CustomFirebaseMessaging : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // update Mapp's token
        Appoxee.instance().updateFirebaseToken(token)

        // update token for other providers
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val isMapp = Appoxee.instance().isPushMessageFromMapp(message)
        if (isMapp) {
            Appoxee.instance().handlePushMessage(message)
        } else {
            // handle with some other provider
        }
    }
}