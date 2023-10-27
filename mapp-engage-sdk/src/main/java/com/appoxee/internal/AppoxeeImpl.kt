@file:Suppress("PrivatePropertyName")

package com.appoxee.internal

import android.app.Application
import android.content.Context
import android.util.Log
import com.appoxee.Appoxee
import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.MappCallback
import com.appoxee.shared.MappResult
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
                mIsReady.set(true)
                updateReadyStatus(true, mappResult.getData())
            }
        }
    }

    init {
        println("OPTIONS: $options")
        saveConfiguration(options)
        validateDeviceRegistration()
    }

    private fun validateDeviceRegistration() {
        safeCall(registerCallback) {
            var modified = false

            // get FCM token
            val pushToken = FirebaseMessaging.getInstance().token.await()

            val registrationDevice =
                appoxeeContainer.deviceProvider.generateRegistrationDevice(pushToken)

            val savedRegistrationDevice = appoxeeContainer.storage.getRegistrationDevice()

            // check & get data device if already registered
            var devicePayload = appoxeeContainer.storage.getDevicePayload()

            if (registrationDevice == savedRegistrationDevice && devicePayload != null) {
                return@safeCall devicePayload
            }

            devicePayload = appoxeeContainer.appoxeeAdapter.getDevice()

            if (devicePayload?.udidHashed == null) {
                // if device not registered already, register device
                Log.i(TAG, "PUSH TOKEN FROM LIB: $pushToken")
                appoxeeContainer.appoxeeAdapter.register(registrationDevice)
                appoxeeContainer.storage.saveRegistrationDevice(registrationDevice)
                modified = true
            }

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
            }

            // save to local storage
            appoxeeContainer.storage.saveDevicePayload(devicePayload)
            appoxeeContainer.storage.saveRegistrationDevice(registrationDevice)

            devicePayload
        }.invokeOnCompletion {
            getAppConfig()
        }
    }

    override fun isReady(): Boolean {
        return mIsReady.get()
    }

    override fun setAlias(alias: String, callback: MappCallback<String>?) {
        safeCall(callback) {
            appoxeeContainer.appoxeeAdapter.setAlias(alias)
        }
    }

    override fun getAlias(callback: MappCallback<String>?) {
        safeCall(callback) {
            appoxeeContainer.appoxeeAdapter.getAlias()
        }
    }

    override fun optIn(token: String, callback: MappCallback<Boolean>?) {
        safeCall(callback) {
            appoxeeContainer.appoxeeAdapter.optIn(pushToken = token)
        }
    }

    override fun optOut(token: String, callback: MappCallback<Boolean>?) {
        safeCall(callback) {
            appoxeeContainer.appoxeeAdapter.optOut(token)
        }
    }

    override fun getDevice(callback: MappCallback<DevicePayload>?) {
        safeCall(callback) {
            appoxeeContainer.appoxeeAdapter.getDevice()
        }
    }


    override fun updateReadyStatus(status: Boolean, devicePayload: DevicePayload?) {
        observers.forEach {
            it.onReadyStatusChanged(status, devicePayload)
        }
    }

    override fun subscribe(observer: AppoxeeObserver) {
        coroutineScope.launch {
            observer.onReadyStatusChanged(isReady(), appoxeeContainer.storage.getDevicePayload())
        }
        observers.add(observer)
    }

    override fun unsubscribe(observer: AppoxeeObserver) {
        observers.remove(observer)
    }

    private fun saveConfiguration(options: AppoxeeOptions) = coroutineScope.launch {
        // TODO Save configuration
    }

    private fun getAppConfig() {
        safeCall(object : MappCallback<AppConfigPayload> {
            override fun onResult(mappResult: MappResult<AppConfigPayload>) {
                if (mappResult.isSuccess()) {
                    Log.d(TAG, "APP CONFIG: ${mappResult.getData()?.toString()}")
                } else {
                    Log.e(TAG, mappResult.getError()?.toString() ?: "Error")
                }
            }
        }) {
            appoxeeContainer.appoxeeAdapter.getAppConfig()
        }
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
                Log.e(TAG, e.toString())
                callback?.onResult(MappResult.Error(e))
            }
        }
    }
}