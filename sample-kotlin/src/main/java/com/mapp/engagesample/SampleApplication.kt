package com.mapp.engagesample

import android.app.Application
import com.appoxee.Appoxee
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.NotificationMode

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val options: AppoxeeOptions = AppoxeeOptions(
            server = AppoxeeOptions.Server.TEST,
            sdkKey = BuildConfig.MAPP_SDK_KEY,
            tenantId = BuildConfig.MAPP_TENANT_ID,
            appId = BuildConfig.MAPP_APP_ID,
        ).also {
            it.notificationMode = NotificationMode.BACKGROUND_AND_FOREGROUND
        }


        Appoxee.engage(this, options)

        Appoxee.instance().setPushBroadcast(MyPushBroadcast::class.java)
    }
}