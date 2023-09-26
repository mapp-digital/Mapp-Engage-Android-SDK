package com.mapp.engagesample;

import android.app.Application;
import android.util.Log;

import com.appoxee.Appoxee;
import com.appoxee.AppoxeeOptions;
import com.appoxee.push.NotificationMode;

public class SampleApplication extends Application {

    private static final String TAG = SampleApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        AppoxeeOptions options = new AppoxeeOptions(AppoxeeOptions.Server.L3, "183408d0cd3632.83592719", "206974", "5963");
        //options.setCepUrl("https://jamie.m.shortest-route.com");
        options.setNotificationMode(NotificationMode.BACKGROUND_AND_FOREGROUND);
        Appoxee.engage(this, options, (success, exception) -> {
            Log.d(TAG, "Success: " + success + "; exception: " + (exception != null ? exception.getMessage() : ""));
        });

//        Appoxee.instance().addInitListener((success, exception) -> {
//            Log.d(TAG, "Success: " + success + "; exception: " + (exception != null ? exception.getMessage() : ""));
//        });
    }
}
