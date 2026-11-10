package com.mapp.engagesample

import android.app.Application
import com.mapp.engagesample.configuration.EngageConfigurationClient
import com.mapp.engagesample.configuration.SdkConfiguration
import com.mapp.engagesample.configuration.SdkConfigurationManager
import com.mapp.engagesample.configuration.SdkConfigurationPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class SampleApplication : Application() {
    internal val sdkConfiguration: SdkConfigurationManager by lazy {
        SdkConfigurationManager(
            defaults = SdkConfiguration.buildDefaults(),
            storage = SdkConfigurationPreferences(getSharedPreferences("sdk_configuration", MODE_PRIVATE)),
            client = EngageConfigurationClient(this),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        )
    }

    override fun onCreate() {
        super.onCreate()
        sdkConfiguration.start()
    }
}
