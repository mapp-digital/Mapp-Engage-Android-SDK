package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData

interface Actions<out T> : NetworkData {
    fun get(): T?
}