package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import org.json.JSONArray
import org.json.JSONObject

internal class GetDeviceModel : NetworkData {

    private val attributes: List<String> = listOf(
        "alias",
        "dmcUserId",
        "pushToken",
        "pushToken_bk",
        "UDIDHashed"
    )

    private lateinit var json: JSONObject

    override fun asJson(): JSONObject {
        if (!::json.isInitialized) {
            json = JSONObject().apply {
                put("get", JSONArray(attributes))
            }
        }
        return json
    }

    override fun asString(): String {
        return asJson().toString()
    }
}