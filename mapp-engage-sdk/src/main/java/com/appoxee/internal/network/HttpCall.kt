package com.appoxee.internal.network

import com.appoxee.internal.network.exceptions.CallConsumedException
import com.appoxee.internal.util.DispatchersProvider
import com.appoxee.internal.util.Logger
import com.appoxee.shared.MappCallback
import com.appoxee.shared.MappResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class HttpCall<T>(
    private val scope: CoroutineScope,
    private val call: suspend () -> T,
    private val dispatchersProvider: DispatchersProvider
) : Call<T> {
    private val mutex = Mutex()

    @Volatile
    private var executed: Boolean = false

    private fun isExecuted(): Boolean {
        return runBlocking {
            mutex.withLock {
                return@runBlocking executed
            }
        }
    }

    override fun execute(): MappResult<T> = runBlocking {
        if (isExecuted()) throw CallConsumedException()
        executeWithErrorHandling()
    }

    override suspend fun asSuspend(): MappResult<T> {
        if (isExecuted()) throw CallConsumedException()
        return withContext(dispatchersProvider.defaultDispatcher) {
            executeWithErrorHandling()
        }
    }

    override fun enqueue(callback: MappCallback<T>?) {
        if (isExecuted()) throw CallConsumedException()
        scope.launch {
            val result = executeWithErrorHandling()
            withContext(dispatchersProvider.mainDispatcher) {
                callback?.onResult(result)
            }
        }
    }

    private suspend fun executeWithErrorHandling(): MappResult<T> {
        return try {
            executed = true
            val result = call.invoke()
            MappResult.Success(result)
        } catch (e: Throwable) {
            Logger.e("HttpCall", e.message ?: "Unknown message")
            MappResult.Error(e)
        } catch (e: Exception) {
            Logger.e("HttpCall", e.message ?: "Unknown message")
            MappResult.Error(e)
        }
    }
}