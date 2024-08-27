package com.mapp.engagesample;

import android.util.Log;

import androidx.annotation.NonNull;

import com.appoxee.shared.LocalPushBroadcast;
import com.appoxee.shared.MappPush;

public class MyPushBroadcast extends LocalPushBroadcast {

    private void print(String action, MappPush push) {
        Log.d("MyPushBroadcast", "Action: " + action + "; Push: " + push);
    }

    @Override
    public void onReceived(@NonNull MappPush push) {
        print("onReceived", push);
    }

    @Override
    public void onOpened(@NonNull MappPush push) {
        print("onOpened", push);
    }

    @Override
    public void onSilent(@NonNull MappPush push) {
        print("onSilent", push);
    }

    @Override
    public void onDismissed(@NonNull MappPush push) {
        print("onDismissed", push);
    }

    @Override
    public void onButtonClick(@NonNull MappPush push) {
        print("onButtonClick", push);
    }

    @Override
    public void onRichPush(@NonNull MappPush push) {
        print("onRichPush", push);
    }
}
