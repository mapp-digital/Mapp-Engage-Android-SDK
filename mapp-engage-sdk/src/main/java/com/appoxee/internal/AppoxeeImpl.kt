@file:Suppress("PrivatePropertyName")

package com.appoxee.internal

import android.app.Application
import android.content.Context
import com.appoxee.Appoxee
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse
import com.appoxee.internal.network.Call
import com.appoxee.internal.network.HttpCall
import com.appoxee.internal.util.Logger
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.MappCallback
import com.appoxee.shared.MappResult
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

internal class AppoxeeImpl(
    context: Context,
    options: AppoxeeOptions,
) : Appoxee, AppoxeeObservable {

    private val TAG = AppoxeeImpl::class.java.name

    private val observers: MutableSet<AppoxeeObserver> = mutableSetOf()

    private val appoxeeContainer =
        AppoxeeContainer(context.applicationContext as Application, options)

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val mIsReady = AtomicBoolean(false)

    private val registerCallback = object : MappCallback<DevicePayload> {
        override fun onResult(mappResult: MappResult<DevicePayload>) {
            if (mappResult.isSuccess()) {
                updateReadyStatus(true, mappResult)
            } else {
                updateReadyStatus(false, mappResult)
            }
        }
    }

    init {
        Logger.init(context.applicationContext as Application)
        println("OPTIONS: $options")
        saveConfiguration(options)
        validateRegistration()
    }

    private fun validateRegistration() {
        safeCall(registerCallback) {
            // get saved device from local storage
            var devicePayload: DevicePayload? = appoxeeContainer.storage.getDevicePayload()

            // get registration data used to register device on server
            val savedRegisterPayload = appoxeeContainer.storage.getRegistrationDevice()

            // calculate current registration data of device
            val newRegisterPayload = appoxeeContainer.deviceProvider.generateRegistrationDevice()

            // if local device payload exist and data are not expired
            if (devicePayload?.udidHashed != null /* && not expired */) {
                // check if saved registration data and currently calculated registration data differs
                if (savedRegisterPayload != newRegisterPayload) {
                    // when registration data differs, register data again to update valus on server
                    appoxeeContainer.appoxeeAdapter.register(newRegisterPayload)

                    // get device payload from server after new registration
                    devicePayload = appoxeeContainer.appoxeeAdapter.getDevice()
                }
            } else {
                // device payload doesn't exist or expired
                // get new device payload from server
                devicePayload = appoxeeContainer.appoxeeAdapter.getDevice()

                // check if device payload from server exist or not
                if (devicePayload?.udidHashed == null) {
                    // if device payload doesn't exist on server, register device
                    appoxeeContainer.appoxeeAdapter.register(newRegisterPayload)

                    // get device payload from server after new registration
                    devicePayload = appoxeeContainer.appoxeeAdapter.getDevice()
                }
            }

            // save device registration data (device fingerprint) to a local storage for later access
            appoxeeContainer.storage.saveRegistrationDevice(newRegisterPayload)

            // save device payload from server for a registered device
            appoxeeContainer.storage.saveDevicePayload(devicePayload)

            // returns device payload
            devicePayload
        }.invokeOnCompletion {
            // when registration is validated
            safeCall(null) {
                // update optIn or optOut status with firebase token
                updateOptStatus()

                // fetch InApp Configuration parameters
                getAppConfig()
            }
        }
    }

    private fun updateOptStatus() {
        safeCall(null) {
            Logger.d(TAG, "updateOptStatus()")
            var devicePayload = appoxeeContainer.storage.getDevicePayload()
            val pushToken = FirebaseMessaging.getInstance().token.await()
            var modified: Boolean = false
            // if device opted Out and optOut token is expired, update optOut token
            if (devicePayload?.pushTokenBk?.isNotEmpty() == true
                && pushToken != devicePayload.pushTokenBk
            ) {
                appoxeeContainer.appoxeeAdapter.optOut(pushToken)
                modified = true
            }

            // if device opted In and optIn token is expired, update optIn token
            if (devicePayload?.pushToken?.isNotEmpty() == true && pushToken != devicePayload.pushToken) {
                appoxeeContainer.appoxeeAdapter.optIn(pushToken)
                modified = true
            }

            if (modified) {
                // get fresh device data from server
                devicePayload = appoxeeContainer.appoxeeAdapter.getDevice()
                appoxeeContainer.storage.saveDevicePayload(devicePayload)
            }

            Logger.d(TAG, "updateOptStatus() - Finished")
        }
    }

    override fun isReady(): Boolean {
        return mIsReady.get()
    }

    override fun setAlias(alias: String): Call<String?> = buildHttpCall {
        appoxeeContainer.appoxeeAdapter.setAlias(alias) ?: ""
    }

    override fun getAlias(): Call<String?> = buildHttpCall {
        appoxeeContainer.appoxeeAdapter.getAlias()
    }

    override fun fetchInboxMessages(eventName: String): Call<InboxMessagesResponse?> =
        buildHttpCall {
            appoxeeContainer.appoxeeAdapter.fetchInboxMessages(eventName)
        }

    override fun fetchInappMessages(eventName: String): Call<InappResponse?> = buildHttpCall {
        appoxeeContainer.appoxeeAdapter.fetchInappMessages(eventName)
    }

    override fun optIn(token: String): Call<Boolean> = buildHttpCall {
        appoxeeContainer.appoxeeAdapter.optIn(pushToken = token)
    }

    override fun optOut(token: String): Call<Boolean> = buildHttpCall {
        appoxeeContainer.appoxeeAdapter.optOut(token)
    }

    override fun getDevice(): Call<DevicePayload?> = buildHttpCall {
        appoxeeContainer.appoxeeAdapter.getDevice()
    }


    override fun updateReadyStatus(status: Boolean, mappResult: MappResult<DevicePayload>) {
        mIsReady.set(status)
        observers.forEach {
            it.onReadyStatusChanged(status, mappResult)
        }
    }

    override fun subscribe(observer: AppoxeeObserver) {
        coroutineScope.launch {
            val payload = appoxeeContainer.storage.getDevicePayload()
            payload?.let {
                observer.onReadyStatusChanged(
                    isReady(),
                    MappResult.Success(data = it)
                )
            }
        }
        observers.add(observer)
    }

    override fun unsubscribe(observer: AppoxeeObserver) {
        observers.remove(observer)
    }

    override fun testCall(): Call<String> = buildHttpCall {
        val start = System.currentTimeMillis()
        delay(5000)
        return@buildHttpCall "Response from testCall after delay of ${System.currentTimeMillis() - start} ms."
    }

    private fun saveConfiguration(options: AppoxeeOptions) = coroutineScope.launch {
        // TODO Save configuration
    }

    private suspend fun getAppConfig() {
        val result = appoxeeContainer.appoxeeAdapter.getAppConfig()
        if (result.isSuccess()) {
            Logger.d(TAG, "APP CONFIG: ${result.data?.toString()}")
        } else {
            Logger.e(TAG, result.error?.toString() ?: "Error")
        }
    }

    private fun <T> buildHttpCall(
        call: suspend () -> T
    ): Call<T> {
        return HttpCall(coroutineScope, call)
    }

    private fun <T> safeCall(
        callback: MappCallback<T>?,
        call: suspend () -> T?
    ) = coroutineScope.launch {
        try {
            val data = call.invoke()
            withContext(Dispatchers.Main) {
                callback?.onResult(MappResult.Success(data))
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Logger.e(TAG, "Appoxee call $call error: $e")
                callback?.onResult(MappResult.Error(e))
            }
        }
    }


}