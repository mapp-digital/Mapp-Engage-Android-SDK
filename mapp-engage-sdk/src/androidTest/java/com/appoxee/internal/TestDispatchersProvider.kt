package com.appoxee.internal

import com.appoxee.internal.util.DispatchersProvider
import kotlinx.coroutines.CoroutineDispatcher

class TestDispatchersProvider(
    override val ioDispatcher: CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
    override val mainDispatcher: CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
    override val defaultDispatcher: CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
) : DispatchersProvider {
}