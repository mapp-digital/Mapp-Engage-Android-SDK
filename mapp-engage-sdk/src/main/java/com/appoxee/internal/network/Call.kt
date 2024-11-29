package com.appoxee.internal.network

import com.appoxee.shared.MappCallback
import com.appoxee.shared.MappResult

/**
 * Defines protocol for a result of a public SDK methods;
 * Public methods returns instance of Call<T> object, and to unwrap result,
 * user needs to execute on of the methods defined in this interface
 */
interface Call<T> {
    /**
     * Execute method and get [MappResult]<[T]> result as a return value
     * Method should be used from Java code, inside some background executor or background thread.
     */
    fun execute(): MappResult<T>

    /**
     * Execute method and get result via callback [MappCallback]<[T]>
     * Method should be executed from a Main [Thread]
     */
    fun enqueue(callback: MappCallback<T>?)

    /**
     * Execute methods and get [MappResult]<[T]> result as a return value
     * Method is intended for usage in a Kotlin applications,
     * and provides convenient solution for getting result from a coroutines.
     */
    @JvmSynthetic
    suspend fun asSuspend(): MappResult<T>
}