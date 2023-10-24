@file:Suppress("PrivatePropertyName")

package com.appoxee.internal

import android.app.Application
import android.content.Context
import android.util.Log
import com.appoxee.Appoxee
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.network.EngageApiImpl
import com.appoxee.internal.network.NetworkClientImpl
import com.appoxee.internal.provider.DeviceProvider
import com.appoxee.internal.provider.DeviceProviderImpl
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.MappCallback
import com.appoxee.shared.MappResult
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

internal class AppoxeeImpl(
    context: Context,
    options: AppoxeeOptions,
) : Appoxee, AppoxeeObservable {

    private val TAG = AppoxeeImpl::class.java.name

    private val observers: MutableSet<AppoxeeObserver> = mutableSetOf()

    /*    private val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            Log.e(TAG, "EXCEPTION IN COROUTINE: $throwable")
        }*/

    private val appoxeeContainer =
        AppoxeeContainer(context.applicationContext as Application, options)

    private val coroutineScope = CoroutineScope(Dispatchers.IO /* + exceptionHandler*/)

    private val mIsReady = AtomicBoolean(false)

    init {
        println("OPTIONS: $options")
        saveConfiguration(options)
        register()
    }


    private fun saveConfiguration(options: AppoxeeOptions) = coroutineScope.launch {
        // TODO Save configuration
    }


    private fun register() =
        coroutineScope.launch {
            val result = safeCall {
                appoxeeContainer.appoxeeAdapter.register()
            }

            if (result.isSuccess()) {
                mIsReady.set(true)
                withContext(Dispatchers.Main) {
                    updateReadyStatus(true)
                }
            }
        }

    override fun subscribeOnReadyChanged(event: (Boolean) -> Unit) {

    }

    override fun isReady(): Boolean {
        return mIsReady.get()
    }

    override fun setAlias(alias: String, callback: MappCallback<String>?) {
        coroutineScope.launch {
            val result = safeCall {
                val data = appoxeeContainer.appoxeeAdapter.setAlias(alias)
                data.payload?.dmcUserId ?: ""
            }
            withContext(Dispatchers.Main) {
                callback?.onResult(result)
            }
        }
    }

    override fun getAlias(callback: MappCallback<String>?) {
        coroutineScope.launch {
            val alias = appoxeeContainer.appoxeeAdapter.getAlias()
            callback?.onResult(MappResult.Success(alias))
        }
    }

    override fun getDevice(callback: MappCallback<DevicePayload>?) {
        coroutineScope.launch {
            val result = safeCall {
                val data = appoxeeContainer.appoxeeAdapter.getDevice()
                data.payload
            }
            withContext(Dispatchers.Main) {
                callback?.onResult(result)
            }
        }
    }

    private inline fun <T> safeCall(
        call: () -> T?
    ): MappResult<T> {
        return try {
            val data = call.invoke()
            MappResult.Success(data)
        } catch (e: Exception) {
            MappResult.Error(e)
        }
    }

    override fun updateReadyStatus(status: Boolean) {
        observers.forEach {
            it.onReadyStatusChanged(status)
        }
    }

    override fun subscribe(observer: AppoxeeObserver) {
        observer.onReadyStatusChanged(isReady())
        observers.add(observer)
    }

    override fun unsubscribe(observer: AppoxeeObserver) {
        observers.remove(observer)
    }
}