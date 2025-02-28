package com.appoxee.internal.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal class DispatchersProviderImpl : com.appoxee.internal.util.DispatchersProvider {
    override val ioDispatcher: CoroutineDispatcher
        get() = Dispatchers.IO
    override val mainDispatcher: CoroutineDispatcher
        get() = Dispatchers.Main
    override val defaultDispatcher: CoroutineDispatcher
        get() = Dispatchers.Default
}