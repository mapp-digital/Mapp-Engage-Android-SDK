package com.appoxee.internal.model.request

internal data class DeviceAttributesModel(val get: List<String>) {
    companion object {
        val default: List<String> =
            listOf("alias", "dmcUserId", "pushToken", "pushToken_bk", "UDIDHashed")
    }
}