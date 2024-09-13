package com.appoxee.internal.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal class DispatchersImpl : com.appoxee.internal.util.Dispatchers {
    override val ioDispatcher: CoroutineDispatcher
        get() = Dispatchers.IO
    override val mainDispatcher: CoroutineDispatcher
        get() = Dispatchers.Main
    override val defaultDispatcher: CoroutineDispatcher
        get() = Dispatchers.Default
}