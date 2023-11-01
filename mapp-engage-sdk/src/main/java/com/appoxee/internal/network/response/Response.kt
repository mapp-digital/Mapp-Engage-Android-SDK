package com.appoxee.internal.network.response

internal abstract class Response<T>(
    val statusCode: Int = 200,
    val data: T? = null,
    val error: Throwable? = null
) {

    fun isSuccess(): Boolean {
        return error == null
    }

    companion object {
        fun <T> success(statusCode: Int, data: T?): Response<T> {
            return Success(statusCode, data)
        }

        fun <T> error(error: Throwable?): Response<T> {
            return Error(error)
        }
    }

    internal class Success<T> constructor(statusCode: Int, data: T?) :
        Response<T>(statusCode = statusCode, data = data, error = null)

    internal class Error<T> constructor(error: Throwable?) : Response<T>(error = error)
}