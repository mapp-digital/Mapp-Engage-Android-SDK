package com.appoxee.internal.model.response.inapp

import com.appoxee.internal.util.getLongOrDefault
import org.json.JSONObject

data class Location(
    val position: Int,
    val height: Int,
    val width: Int
) {
    companion object {
        fun fromJSON(json: JSONObject): Location {
            return Location(
                position = json.getLongOrDefault("position", 0).toInt(),
                height = json.getLongOrDefault("height", 0).toInt(),
                width = json.getLongOrDefault("width", 0).toInt()
            )
        }
    }
}
