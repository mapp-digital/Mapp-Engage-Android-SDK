package com.appoxee.internal.network.response

import com.appoxee.internal.model.response.ResponseData
import org.json.JSONObject

internal abstract class Response<T>(
    val statusCode: Int = 200,
    val data: JSONObject? = null,
    val error: Throwable? = null
) {
    abstract fun parse(parser: (JSONObject) -> T): ResponseData<T>
}