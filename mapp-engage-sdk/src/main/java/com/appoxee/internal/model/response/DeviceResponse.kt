package com.appoxee.internal.model.response

internal data class DeviceResponse(val get: DeviceAttributes? = null) {
}

internal data class DeviceAttributes(
    val dmcUserId: String? = null /* Unique user id */,
    val udidHashed: String? = null /* UDIDHashed */,
    val pushTokenBk: String? = null /* OptOut Token */,
    val pushToken: String? = null /* Opt In Token*/,
    val alias: String? = null /* User Alias */
) {
}