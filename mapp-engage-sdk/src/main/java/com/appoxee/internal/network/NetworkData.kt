package com.appoxee.internal.network

import org.json.JSONObject

interface NetworkData {
    fun asJson(): JSONObject

    fun asString(): String
}