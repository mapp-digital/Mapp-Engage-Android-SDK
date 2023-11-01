package com.appoxee.internal.network

import com.appoxee.shared.MappCallback
import com.appoxee.shared.MappResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class HttpCall<T>(
    private val coroutineScope: CoroutineScope,
    private inline val call: suspend () -> T,
) : Call<T> {
    private val mutex = Mutex()

    @Volatile
    private var executed: Boolean = false

    override fun isExecuted(): Boolean {
        return runBlocking {
            mutex.withLock {
                return@runBlocking executed
            }
        }
    }

    override fun execute(): MappResult<T> = runBlocking {
        executed = true
        val result = call.invoke()
        withContext(Dispatchers.Main) {
            MappResult.Success(result)
        }
    }

    override suspend fun asSuspend(): MappResult<T> = withContext(Dispatchers.IO) {
        executed = true
        val result = call.invoke()
        withContext(Dispatchers.Main) {
            MappResult.Success(result)
        }
    }

    override fun enqueue(callback: MappCallback<T>) {
        coroutineScope.launch {
            val response = call.invoke()
            withContext(Dispatchers.Main) {
                callback.onResult(MappResult.Success(response))
            }
        }
    }
}