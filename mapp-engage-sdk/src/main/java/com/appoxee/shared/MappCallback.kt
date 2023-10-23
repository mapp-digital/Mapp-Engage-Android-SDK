package com.appoxee.shared

interface MappCallback<T> {
    fun onResult(mappResult: MappResult<T>)
}