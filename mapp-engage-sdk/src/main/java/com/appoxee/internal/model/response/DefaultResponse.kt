package com.appoxee.internal.model.response

internal data class DefaultResponse(
    val set: List<String> = emptyList(),
    val dmcUserId: String? = null
) {
}