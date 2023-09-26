package com.appoxee.internal

import android.content.Context
import com.appoxee.Appoxee
import com.appoxee.AppoxeeOptions
import com.appoxee.MappCallback
import com.appoxee.MappResult
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

internal class AppoxeeImpl(
    context: Context,
    private val options: AppoxeeOptions,
    private var onInitCompleteListener: Appoxee.OnInitCompleteListener? = null
) : Appoxee {

    private val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        println("EXCEPTION IN COROUTINE: $throwable")
    }
    private val appoxeeAdapter: AppoxeeAdapter
    private val coroutineScope =
        CoroutineScope(Dispatchers.IO)


    private var mIsReady = AtomicBoolean(false)

    init {
        println("OPTIONS: $options")
        saveConfiguration(options)
        appoxeeAdapter = AppoxeeAdapter(context, options)
        register()
    }


    private fun saveConfiguration(options: AppoxeeOptions) = coroutineScope.launch {
        // TODO Save configuration
    }


    private fun register() =
        coroutineScope.launch {
            try {
                val registerResponse = appoxeeAdapter.register()
                println(registerResponse)
                mIsReady.set(true)
                onInitCompleteListener?.onInitCompleted(true)
            } catch (e: Throwable) {
                println(e)
            } catch (e: Exception) {
                println(e)
            }
        }

    @Deprecated(message = "Only for backward compatibility. Attach init listener on [engage()] method.")
    fun addInitListener(onInitListener: Appoxee.OnInitCompleteListener) {
        onInitCompleteListener = onInitListener
    }

    override fun isReady(): Boolean {
        return mIsReady.get()
    }

    override fun setAlias(alias: String, callback: MappCallback<Boolean>?) {
        coroutineScope.launch {
            val success = appoxeeAdapter.setAlias(alias)
            callback?.onResult(MappResult.Success(success))
        }
    }

    override fun getAlias(callback: MappCallback<String>?) {
        coroutineScope.launch {
            val alias = appoxeeAdapter.getAlias()
            callback?.onResult(MappResult.Success(alias))
        }
    }
}