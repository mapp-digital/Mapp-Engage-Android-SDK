package com.appoxee.internal.model.common

import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class CustomAttributesCache(
    val attributes: Map<String, Any?>,
    val timestamp: Long = System.currentTimeMillis()
) {

    companion object {
        private val TTL = TimeUnit.HOURS.toMillis(1)
        private val ATTRIBUTES_KEY = "attributes"
        private val TIMESTAMP_KEY = "timestamp"

        fun fromJson(json: JSONObject): CustomAttributesCache {
            val timestamp = json.optLong(TIMESTAMP_KEY)
            val customAttributes = mutableMapOf<String, String?>()
            json.optJSONObject(ATTRIBUTES_KEY)?.let { attributes ->
                attributes.keys().forEach { key ->
                    customAttributes.put(key, attributes.optString(key))
                }
            }

            return CustomAttributesCache(attributes = customAttributes, timestamp = timestamp)
        }
    }

    fun isCacheValid(): Boolean = (System.currentTimeMillis() - timestamp) < TTL

    fun toJson(): JSONObject {
        return JSONObject().also {
            it.put(TIMESTAMP_KEY, System.currentTimeMillis())
            it.put(ATTRIBUTES_KEY, JSONObject().also { innerJson ->
                attributes.forEach { (key, value) ->
                    innerJson.put(key, value?.toString())
                }
            })
        }
    }
}
