package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import org.json.JSONArray
import org.json.JSONObject


internal enum class TagsAction(val value: String) {
    SET("set"),
    REMOVE("remove")
}

internal class Tags(private val tags: List<String>, private val action: TagsAction) : NetworkData {
    private lateinit var json: JSONObject
    override fun asJson(): JSONObject {
        if (!::json.isInitialized) {
            json = JSONObject().apply {
                put("tags", JSONObject().apply {
                    put(action.value, JSONArray(tags))
                })
            }
        }
        return json
    }

    override fun asString(): String {
        return json.toString()
    }
}