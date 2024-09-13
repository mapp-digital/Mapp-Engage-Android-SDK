package com.appoxee.internal

import com.appoxee.internal.util.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher

class TestDispatchers(
    override val ioDispatcher: CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
    override val mainDispatcher: CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
    override val defaultDispatcher: CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
) : Dispatchers {
}