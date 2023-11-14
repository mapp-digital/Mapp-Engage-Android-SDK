package com.appoxee.internal.model.request.geo

import com.appoxee.internal.network.NetworkData
import org.json.JSONObject

internal data class GetRegions(
    private val latitude: Double,
    private val longitude: Double,
    private val version: Int,
    private val applicationId: Long,
    private val pageSize: Int,
) : NetworkData {

    private lateinit var json: JSONObject
    override fun asJson(): JSONObject {
        if (!::json.isInitialized) {
            val region = JSONObject().apply {
                put("latitude", latitude)
                put("longitude", longitude)
                put("version", version)
                put("application_id", applicationId)
                put("page_size", pageSize)
            }

            json = JSONObject().apply {
                put("get_regions", region)
            }
        }
        return json
    }

    override fun asString(): String {
        return asJson().toString()
    }
}