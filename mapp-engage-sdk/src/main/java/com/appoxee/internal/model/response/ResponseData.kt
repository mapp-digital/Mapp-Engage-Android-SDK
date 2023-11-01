package com.appoxee.internal.model.response

import org.json.JSONObject

internal data class ResponseData<out T>(
    val metadata: Metadata? = null,
    val payload: T? = null
) {

    companion object {
        fun <T> fromJSON(json: JSONObject, payloadParser: (JSONObject) -> T): ResponseData<T> {
            val metadata = json.getJSONObject("metadata").let {
                Metadata.fromJSON(it)
            }

            val payload = json.getJSONObject("payload").let {
                payloadParser.invoke(it)
            }

            return ResponseData(metadata, payload)
        }
    }

    override fun toString(): String {
        return "ResponseData(metadata=$metadata, payload=$payload)"
    }
}