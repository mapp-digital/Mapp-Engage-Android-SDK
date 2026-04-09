package com.mapp.engagesample

import android.app.Application
import com.appoxee.Appoxee
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.NotificationMode

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val options: AppoxeeOptions = AppoxeeOptions(
            server = AppoxeeOptions.Server.L3,
            sdkKey = "183408d0cd3632.83592719",
            tenantId = "5963",
            appId = "206974",
        ).also {
            it.notificationMode = NotificationMode.SILENT_ONLY
        }


        Appoxee.engage(this, options)

        Appoxee.instance().setPushBroadcast(MyPushBroadcast::class.java)
    }
}