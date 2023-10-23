package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import org.json.JSONObject

internal data class DeviceModel(val get: List<String>) : NetworkData {
    private val attributes: List<String> =
        listOf("alias", "dmcUserId", "pushToken", "pushToken_bk", "UDIDHashed")

    override fun asJson(): JSONObject {
        val json = JSONObject().apply {
            put("get", attributes)
        }
        return json
    }

    override fun asString(): String {
        return asJson().toString()
    }
}