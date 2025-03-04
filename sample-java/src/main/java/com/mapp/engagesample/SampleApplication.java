package com.mapp.engagesample;

import android.app.Application;
import android.util.Log;

import com.appoxee.Appoxee;
import com.appoxee.shared.AppoxeeOptions;
import com.appoxee.shared.NotificationMode;

public class SampleApplication extends Application {

    private static final String TAG = SampleApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() START");
        super.onCreate();
        AppoxeeOptions options = new AppoxeeOptions(
                AppoxeeOptions.Server.L3,
                "183408d0cd3632.83592719",
                "206974",
                "5963"
        );
        options.setConnectionTimeout(20_000);
        options.setReadTimeout(20_000);
        options.setLogType(AppoxeeOptions.LogLevel.DEBUG);
        options.setNotificationMode(NotificationMode.BACKGROUND_AND_FOREGROUND);
        Appoxee.engage(this, options);

        Appoxee.instance().setPushBroadcast(MyPushBroadcast.class);
    }
}
