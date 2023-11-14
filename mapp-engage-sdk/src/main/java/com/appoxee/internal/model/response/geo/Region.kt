package com.appoxee.internal.model.response.geo

import com.appoxee.internal.util.getLongOrDefault
import com.appoxee.internal.util.getStringOrEmpty
import org.json.JSONObject
import kotlin.math.ln

data class Region(
    private val id: Long,
    private val lat: Double,
    private val lng: Double,
    private val radius: Long,
    private val name: String,
    private val durationFrom: Long,
    private val durationTo: Long,
) {
    fun getId(): Long = id
    fun getName(): String = name

    fun getLat(): Double = lat

    fun getLng(): Double = lng

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