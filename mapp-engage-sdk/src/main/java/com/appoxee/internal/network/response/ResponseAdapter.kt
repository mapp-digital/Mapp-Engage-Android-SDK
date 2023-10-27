package com.appoxee.internal.network.response

import org.json.JSONObject

internal interface ResponseAdapter {
    fun <T> createResponse(statusCode: Int, data: JSONObject?, error: Throwable?): Response<T>
}