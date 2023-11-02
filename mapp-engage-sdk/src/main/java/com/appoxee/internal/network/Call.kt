package com.appoxee.internal.network

import com.appoxee.shared.MappCallback
import com.appoxee.shared.MappResult

interface Call<T> {
    fun execute(): MappResult<T>
    fun enqueue(callback: MappCallback<T>)

    @JvmSynthetic
    suspend fun asSuspend(): MappResult<T>
}