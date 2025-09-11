package com.appoxee.internal.model.response.attributes

import org.json.JSONObject

class CustomAttributesPayload(private val attributes: Map<String, Any?>) {

    fun toJson(): JSONObject {
        return JSONObject().apply {
            attributes.forEach {
                put(it.key, it.value)
            }
        }
    }

    companion object {
        fun fromJson(json: JSONObject): Map<String, Any?> {
            val data = mutableMapOf<String, Any?>()
            json.optJSONObject("get")?.let { innerJson ->
                innerJson.keys().forEach {
                    val value = innerJson.opt(it)
                    data.put(it, value)
                }
            }
            return data
        }
    }
}