package com.appoxee.shared

fun interface MappCallback<T> {
    fun onResult(result: MappResult<T>)
}