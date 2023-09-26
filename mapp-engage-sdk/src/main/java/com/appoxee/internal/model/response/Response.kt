package com.appoxee.internal.model.response

internal data class Response<T>(
    val metadata: Metadata? = null,
    val payload: Payload<T>? = null
) {
}