package com.appoxee.internal.network

import org.json.JSONObject

class RequestBody(private val jsonObject: JSONObject) : NetworkData {
    override fun asJson(): JSONObject {
        TODO("Not yet implemented")
    }

    override fun asString(): String {
        TODO("Not yet implemented")
    }
}