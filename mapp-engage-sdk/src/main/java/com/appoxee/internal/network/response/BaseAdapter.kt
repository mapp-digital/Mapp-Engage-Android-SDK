package com.appoxee.internal.network.response

import com.appoxee.internal.model.response.ResponseData
import org.json.JSONObject

internal class BaseAdapter<T>(private val parser: (JSONObject) -> T) :
    ResponseAdapter<ResponseData<T>> {
    override fun createResponse(
        statusCode: Int,
        data: JSONObject?,
        error: Throwable?
    ): Response<ResponseData<T>> {
        if(error!=null || statusCode !in 200..299){
            return Response.error(error)
        }else {
            val json: JSONObject = data ?: return Response.error(error)
            val responseData: ResponseData<T> = ResponseData.fromJSON(json, payloadParser = parser)
            return if (responseData.metadata?.error == true) {
                Response.error(Throwable(responseData.metadata.errorMessage))
            } else {
                Response.success(statusCode, responseData)
            }
        }
    }
}