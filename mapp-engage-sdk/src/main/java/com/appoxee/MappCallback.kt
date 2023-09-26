package com.appoxee

interface MappCallback<T> {
    fun onResult(mappResult: MappResult<T>)
}