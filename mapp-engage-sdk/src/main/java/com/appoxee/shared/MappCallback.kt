package com.appoxee.shared

interface MappCallback<T> {
    fun onResult(result: MappResult<T>)
}