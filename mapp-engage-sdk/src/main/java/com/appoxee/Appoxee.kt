package com.appoxee

import android.content.Context
import com.appoxee.internal.AppoxeeAdapter
import com.appoxee.internal.AppoxeeImpl
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

interface Appoxee {
    companion object {
        private lateinit var mInstance: Appoxee

        @JvmStatic
        fun engage(
            context: Context,
            options: AppoxeeOptions,
            onInitCompleteListener: OnInitCompleteListener? = null
        ) {
            mInstance = AppoxeeImpl(context.applicationContext, options, onInitCompleteListener)
        }

        @JvmStatic
        fun instance(): Appoxee {
            if (!::mInstance.isInitialized) {
                throw NullPointerException("Must call engage() first!!!")
            }
            return mInstance
        }
    }

    interface OnInitCompleteListener {
        fun onInitCompleted(successful: Boolean, failReason: Exception? = null)
    }

    fun isReady(): Boolean

    fun setAlias(alias: String, callback: MappCallback<Boolean>? = null)

    fun getAlias(callback: MappCallback<String>? = null)
}