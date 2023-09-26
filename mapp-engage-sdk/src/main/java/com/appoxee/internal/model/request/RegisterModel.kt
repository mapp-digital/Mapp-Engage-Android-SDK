package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import org.json.JSONObject

internal data class RegisterModel(
    val osName: String? = "N/A",
    val pushToken: String? = "N/A",
    val appVersion: String? = "N/A",
    val clientVersion: String? = "N/A",
    val locale: String? = "N/A",
    val timeZone: String? = "N/A",
    val hardwareType: String? = "N/A",
    val density: String? = "N/A",
    val vendorID: String? = "N/A",
    val osNumber: String? = "N/A",
    val resolution: String? = "N/A",
) : NetworkData {

    override fun asJson(): JSONObject {
        return JSONObject().put("osName", osName)
            .put("pushToken", pushToken)
            .put("appVersion", appVersion)
            .put("clientVersion", clientVersion)
            .put("locale", locale)
            .put("timeZone", timeZone)
            .put("hardwareType", hardwareType)
            .put("density", density)
            .put("vendorID", vendorID)
            .put("osNumber", osNumber)
            .put("resolution", resolution)
    }

    override fun asString(): String {
        return asJson().toString()
    }
}