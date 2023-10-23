package com.appoxee.internal.model.response

internal data class ResponseData<out T>(
    val metadata: Metadata? = null,
    val payload: T? = null
) {
    override fun toString(): String {
        return "ResponseData(metadata=$metadata, payload=$payload)"
    }
}