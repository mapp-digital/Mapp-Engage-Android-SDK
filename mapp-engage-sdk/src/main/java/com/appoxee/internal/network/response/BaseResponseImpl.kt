package com.appoxee.internal.network.response

import com.appoxee.internal.model.response.Metadata
import com.appoxee.internal.model.response.ResponseData
import org.json.JSONObject

internal class BaseResponseImpl<T>(
    statusCode: Int,
    data: JSONObject? = null,
    error: Throwable? = null,
) :
    Response<T>(statusCode, data, error) {

    override fun parse(parser: (JSONObject) -> T): ResponseData<T> {
        val metadata = data?.getJSONObject("metadata")?.let {
            Metadata(it)
        }

        val payload = data?.getJSONObject("payload")?.let {
            parser(it)
        }

        return ResponseData(metadata, payload)
    }
}