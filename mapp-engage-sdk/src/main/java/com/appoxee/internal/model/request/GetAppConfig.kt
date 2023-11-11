package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import org.json.JSONObject

class GetAppConfig : NetworkData {
    private lateinit var json: JSONObject
    override fun asJson(): JSONObject {
        if (!::json.isInitialized) {
            json = JSONObject().apply {
                put("app_conf", JSONObject())
            }
        }
        return json
    }

    override fun asString(): String {
        return asJson().toString()
    }
}