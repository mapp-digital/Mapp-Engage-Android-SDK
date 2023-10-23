package com.appoxee.internal.network.exceptions

class RedirectException(
    private val code: Int, override val message: String,
    override val cause: Throwable?
) :
    Exception(message, cause) {
    override fun toString(): String {
        return "Code: $code; Message: $message; ${cause?.stackTraceToString()}"
    }
}