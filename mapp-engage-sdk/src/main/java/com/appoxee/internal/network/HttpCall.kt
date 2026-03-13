package com.appoxee.internal.network

import com.appoxee.internal.network.exceptions.CallConsumedException
import com.appoxee.internal.util.DispatchersProvider
import com.appoxee.internal.util.Logger
import com.appoxee.shared.MappCallback
import com.appoxee.shared.MappResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

internal class HttpCall<T>(
    private val scope: CoroutineScope,
    private val call: suspend () -> T,
    private val dispatchersProvider: DispatchersProvider
) : Call<T> {
    private val executed = AtomicBoolean(false)

    private fun markAsExecuted(): Boolean {
        return !executed.compareAndSet(false, true)
    }

    override fun execute(): MappResult<T> = runBlocking {
        if (markAsExecuted()) throw CallConsumedException()
        executeWithErrorHandling()
    }

    override suspend fun asSuspend(): MappResult<T> {
        if (markAsExecuted()) throw CallConsumedException()
        return withContext(dispatchersProvider.defaultDispatcher) {
            executeWithErrorHandling()
        }
    }

    override fun enqueue(callback: MappCallback<T>?) {
        if (markAsExecuted()) {
            scope.launch {
                withContext(dispatchersProvider.mainDispatcher) {
                    callback?.onResult(MappResult.Error(CallConsumedException()))
                }
            }
            return
        }
        scope.launch {
            val result = withContext(dispatchersProvider.defaultDispatcher) {
                executeWithErrorHandling()
            }
            withContext(dispatchersProvider.mainDispatcher) {
                callback?.onResult(result)
            }
        }
    }

    private suspend fun executeWithErrorHandling(): MappResult<T> {
        return try {
            val result = call.invoke()
            MappResult.Success(result)
        } catch (e: Throwable) {
            Logger.e("HttpCall", e.message ?: "Unknown message")
            MappResult.Error(e)
        }
    }
}
