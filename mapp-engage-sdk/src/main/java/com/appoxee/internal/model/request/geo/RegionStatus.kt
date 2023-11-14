package com.appoxee.internal.model.request.geo

import com.appoxee.internal.network.NetworkData
import org.json.JSONObject

data class RegionStatus(
    private val timestamp: Long,
    private val geoEvent: GeoEvent,
    private val dmcUserId: String,
    private val latitude: Double,
    private val longitude: Double,
    private val regionId: Long,
    private val timeZone: String,
    private val version: Int,
    private val applicationId: String,
) : NetworkData {
    private lateinit var json: JSONObject
    override fun asJson(): JSONObject {
        if (!::json.isInitialized) {
            val regionStatus = JSONObject().apply {
                put("timeStamp", timeZone)
                put("event_type", geoEvent.ordinal)
                put("dmc_user_id", dmcUserId)
                put("latitude", latitude)
                put("longitude", longitude)
                put("region_id", regionId)
                put("time_zone", timeZone)
                put("version", version)
                put("application_id", applicationId)
            }
            json=JSONObject().apply {
                put("region_status",regionStatus)
            }
        }
        return json
    }
    override fun asString(): String {
        return asJson().toString()
    }
}
