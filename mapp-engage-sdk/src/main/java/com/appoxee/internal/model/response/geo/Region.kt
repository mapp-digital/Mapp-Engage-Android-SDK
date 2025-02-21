package com.appoxee.internal.model.response.geo

import com.appoxee.internal.util.getLongOrDefault
import com.appoxee.internal.util.getStringOrEmpty
import org.json.JSONObject

data class Region(
    val id: Long,
    val lat: Double,
    val lng: Double,
    val radius: Long,
    val name: String,
    val durationFrom: Long,
    val durationTo: Long,
) {
    private lateinit var json: JSONObject
    fun toJSON(): JSONObject {
        if (!::json.isInitialized) {
            json = JSONObject().apply {
                put("id", id)
                put("lat", lat)
                put("lng", lng)
                put("radius", radius)
                put("name", name)
                put("durationFrom", durationFrom)
                put("durationTo", durationTo)
            }
        }
        return json
    }

    companion object {
        fun fromJSON(json: JSONObject): Region {
            return Region(
                id = json.getLongOrDefault("id"),
                lat = json.getDouble("lat"),
                lng = json.getDouble("lng"),
                radius = json.getLongOrDefault("radius"),
                name = json.getStringOrEmpty("name"),
                durationFrom = json.getLongOrDefault("durationFrom"),
                durationTo = json.getLongOrDefault("durationTo")
            )
        }
    }
}