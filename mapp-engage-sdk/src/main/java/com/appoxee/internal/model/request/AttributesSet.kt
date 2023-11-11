package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import org.json.JSONObject
import java.util.Date
import java.util.UUID

class AttributesSet(
    private val attributes: Map<String, Any?>
) :
    NetworkData {
    private lateinit var json: JSONObject
    override fun asJson(): JSONObject {
        if (!::json.isInitialized) {
            json = JSONObject().apply {
                put("set", JSONObject().apply {
                    attributes.map {
                        when (it.value) {
                            is Number -> {
                                put(it.key, it.value as Number)
                            }

                            is Boolean -> {
                                put(it.key, it.value as Boolean)
                            }

                            is Date -> {
                                put(it.key, it.value as Date)
                            }

                            is String -> {
                                put(it.key, it.value.toString())
                            }

                            else -> {
                                put(it.key, null)
                            }
                        }
                    }
                })
            }
        }
        return json
    }

    override fun asString(): String {
        return json.toString()
    }
}