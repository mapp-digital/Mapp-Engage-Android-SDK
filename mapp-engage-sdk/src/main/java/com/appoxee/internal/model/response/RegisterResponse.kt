package com.appoxee.internal.model.response

internal data class RegisterResponse(
    var dmcUserId: String?,
    var register: List<String>
) {
}