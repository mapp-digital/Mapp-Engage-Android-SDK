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
                BuildConfig.MAPP_SDK_KEY,
                BuildConfig.MAPP_APP_ID,
                BuildConfig.MAPP_TENANT_ID
        );
        options.setNotificationMode(NotificationMode.BACKGROUND_AND_FOREGROUND);
        Appoxee.engage(this, options);

        Appoxee.instance().setPushBroadcast(MyPushBroadcast.class);
    }
}
