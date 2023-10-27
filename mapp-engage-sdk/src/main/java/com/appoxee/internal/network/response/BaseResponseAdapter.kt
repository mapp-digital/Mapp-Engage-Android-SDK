package com.appoxee.internal.network.response

import org.json.JSONObject

internal class BaseResponseAdapter : ResponseAdapter {
    override fun <T> createResponse(
        statusCode: Int,
        data: JSONObject?,
        error: Throwable?
    ): Response<T> {
        return BaseResponseImpl<T>(statusCode, data, error)
    }
}