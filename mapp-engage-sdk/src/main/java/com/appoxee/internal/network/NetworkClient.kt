package com.appoxee.internal.network

import org.json.JSONObject
import java.net.HttpURLConnection

interface NetworkClient {
    suspend fun execute(request: Request): JSONObject?
}