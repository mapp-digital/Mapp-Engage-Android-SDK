package com.appoxee.internal.network

import org.json.JSONObject

internal interface NetworkClient {
    suspend fun execute(request: Request): JSONObject?
}