package com.appoxee.internal.util

import kotlinx.coroutines.CoroutineDispatcher

internal interface Dispatchers {
    val ioDispatcher: CoroutineDispatcher
    val mainDispatcher: CoroutineDispatcher
    val defaultDispatcher: CoroutineDispatcher
}