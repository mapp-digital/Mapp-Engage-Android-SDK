package com.appoxee.internal.network

import org.json.JSONObject

internal interface NetworkData {
    fun asJson(): JSONObject
    fun asString(): String
}