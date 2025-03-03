package com.appoxee.internal.util

import kotlinx.coroutines.CoroutineDispatcher

internal interface DispatchersProvider {
    val ioDispatcher: CoroutineDispatcher
    val mainDispatcher: CoroutineDispatcher
    val defaultDispatcher: CoroutineDispatcher
}