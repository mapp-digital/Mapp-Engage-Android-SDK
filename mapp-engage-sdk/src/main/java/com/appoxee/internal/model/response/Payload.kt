package com.appoxee.internal.model.response

internal data class Payload<T>(private val payload: T) {
    fun get(): T {
        return payload
    }
}