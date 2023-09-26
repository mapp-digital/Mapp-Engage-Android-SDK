package com.appoxee

abstract class MappResult<T>(
    private val data: T? = null,
    private val status: Status,
    private val throwable: Throwable?
) {

    fun isSuccess(): Boolean = status == Status.SUCCESS

    fun getData(): T? = data

    fun getError(): Throwable? = throwable

    class Success<T>(data: T? = null) :
        MappResult<T>(data = data, status = Status.SUCCESS, throwable = null)

    class Error<T>(throwable: Throwable? = null) :
        MappResult<T>(data = null, status = Status.ERROR, throwable = throwable)

    enum class Status(value: String) {
        SUCCESS("success"),
        ERROR("error")
    }
}