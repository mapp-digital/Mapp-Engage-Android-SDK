package com.appoxee.internal.network.response

import com.appoxee.internal.model.response.ResponseData
import org.json.JSONObject

internal class StatusAdapter() : ResponseAdapter<ResponseData<Boolean>> {
    override fun createResponse(
        statusCode: Int,
        data: JSONObject?,
        error: Throwable?
    ): Response<ResponseData<Boolean>> {
        return if (statusCode == 200) Response.success(
            200,
            ResponseData(null, true)
        ) else Response.error(error)
    }
}