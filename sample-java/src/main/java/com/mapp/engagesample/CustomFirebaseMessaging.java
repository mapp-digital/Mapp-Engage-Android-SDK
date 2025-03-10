package com.mapp.engagesample;

import androidx.annotation.NonNull;

import com.appoxee.Appoxee;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class CustomFirebaseMessaging extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Appoxee.instance().updateFirebaseToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        boolean isMapp= Appoxee.instance().isPushMessageFromMapp(message);
        if(isMapp){
            Appoxee.instance().handlePushMessage(message);
        }else{
            // handle messages for other providers
        }
    }
}
