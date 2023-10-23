package com.appoxee.internal.network.exceptions

class UnknownNetworkException(override val message: String, override val cause: Throwable?) :
    Exception(message, cause) {
    override fun toString(): String {
        return "Message: $message; ${cause?.stackTraceToString()}"
    }
}