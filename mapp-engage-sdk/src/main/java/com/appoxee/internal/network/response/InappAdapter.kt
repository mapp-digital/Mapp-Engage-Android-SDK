package com.appoxee.internal.network.response

import com.appoxee.internal.model.response.inapp.InappResponse
import org.json.JSONObject

internal class InappAdapter : ResponseAdapter<InappResponse> {
    override fun createResponse(
        statusCode: Int,
        data: JSONObject?,
        error: Throwable?
    ): Response<InappResponse> {
        val json = data ?: return Response.error(error)
        val inapp = InappResponse.fromJSON(json)
        return Response.success(statusCode, inapp)
    }
}