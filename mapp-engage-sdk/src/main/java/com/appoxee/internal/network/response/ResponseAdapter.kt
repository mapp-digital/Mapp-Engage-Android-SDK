package com.appoxee.internal.network.response

import org.json.JSONObject

internal fun interface ResponseAdapter<T> {
    fun createResponse(statusCode: Int, data: JSONObject?, error: Throwable?): Response<T>
}