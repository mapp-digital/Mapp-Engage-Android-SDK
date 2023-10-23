package com.appoxee

import android.content.Context
import com.appoxee.internal.AppoxeeImpl
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.MappCallback

interface Appoxee {
    companion object {
        private lateinit var mInstance: Appoxee

        @JvmStatic
        fun engage(
            context: Context,
            options: AppoxeeOptions,
        ) {
            mInstance = AppoxeeImpl(context.applicationContext, options)
        }

        @JvmStatic
        fun instance(): Appoxee {
            if (!::mInstance.isInitialized) {
                throw NullPointerException("Must call engage() first!!!")
            }
            return mInstance
        }
    }

    fun isReady(): Boolean

    fun subscribeOnReadyChanged(event: (Boolean) -> Unit)

    fun getDevice(callback: MappCallback<DevicePayload>?)

    fun setAlias(alias: String, callback: MappCallback<String>? = null)

    fun getAlias(callback: MappCallback<String>? = null)

    fun subscribe(observer: AppoxeeObserver)

    fun unsubscribe(observer: AppoxeeObserver)
}