package com.appoxee.internal.network.response

import com.appoxee.internal.model.response.inbox.InboxMessagesResponse
import org.json.JSONObject

internal class InboxAdapter() : ResponseAdapter<InboxMessagesResponse> {
    override fun createResponse(
        statusCode: Int,
        data: JSONObject?,
        error: Throwable?
    ): Response<InboxMessagesResponse> {
        val json = data ?: return Response.error(error)
        val inbox = InboxMessagesResponse.fromJSON(json)
        return Response.success(statusCode, inbox)
    }
}