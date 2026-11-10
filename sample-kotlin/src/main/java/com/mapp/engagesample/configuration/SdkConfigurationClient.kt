package com.mapp.engagesample.configuration

import android.app.Application
import com.appoxee.Appoxee
import com.appoxee.shared.AppoxeeObserver
import com.mapp.engagesample.MyPushBroadcast

internal interface SdkConfigurationClient {
    fun apply(configuration: SdkConfiguration, onStatus: (Boolean, Throwable?) -> Unit)
    fun isReady(): Boolean
}

/** One application-owned observer; no Fragment or Activity is retained. */
internal class EngageConfigurationClient(private val application: Application) : SdkConfigurationClient {
    private var observedSdk: Appoxee? = null
    @Volatile
    private var listener: ((Boolean, Throwable?) -> Unit)? = null
    private val observer = AppoxeeObserver { ready, result ->
        val error = result.getError()
            ?: if (!result.isSuccess()) IllegalStateException("SDK initialization failed") else null
        listener?.invoke(ready && result.isSuccess(), error)
    }

    override fun apply(configuration: SdkConfiguration, onStatus: (Boolean, Throwable?) -> Unit) {
        listener = onStatus
        Appoxee.engage(application, configuration.toOptions())
        val sdk = Appoxee.instance()
        sdk.setPushBroadcast(MyPushBroadcast::class.java)
        if (observedSdk !== sdk) {
            observedSdk?.unsubscribe(observer)
            observedSdk = sdk
            sdk.subscribe(observer)
        }
    }

    override fun isReady(): Boolean = observedSdk?.isReady() == true
}
