package com.appoxee.internal.network

import com.appoxee.internal.network.exceptions.CallConsumedException
import com.appoxee.shared.MappCallback
import com.appoxee.shared.MappResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class HttpCall<T>(
    private val coroutineScope: CoroutineScope,
    private val call: suspend () -> T,
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
        return@runBlocking executeWithErrorHandling()
    }

    override suspend fun asSuspend(): MappResult<T> = withContext(Dispatchers.IO) {
        if (isExecuted()) throw CallConsumedException()
        return@withContext executeWithErrorHandling()
    }

    override fun enqueue(callback: MappCallback<T>?) {
        if (isExecuted()) throw CallConsumedException()
        coroutineScope.launch {
            val result = executeWithErrorHandling()
            withContext(Dispatchers.Main) {
                callback?.onResult(result)
            }
        }
    }

    private suspend fun executeWithErrorHandling(): MappResult<T> {
        return try {
            executed = true
            val result = call.invoke()
            withContext(Dispatchers.Main) {
                MappResult.Success(result)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                MappResult.Error(e)
            }
        }
    }
}