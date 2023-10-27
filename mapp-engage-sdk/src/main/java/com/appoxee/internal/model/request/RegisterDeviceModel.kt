package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import com.appoxee.internal.util.getNullableString
import org.json.JSONObject

internal data class RegisterDeviceModel(
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

    private lateinit var json: JSONObject

    companion object {
        fun fromJSON(json: JSONObject): RegisterDeviceModel {
            return RegisterDeviceModel(
                osName = json.getNullableString("osName"),
                pushToken = json.getNullableString("pushToken"),
                appVersion = json.getNullableString("appVersion"),
                clientVersion = json.getNullableString("clientVersion"),
                locale = json.getNullableString("locale"),
                timeZone = json.getNullableString("timeZone"),
                hardwareType = json.getNullableString("hardwareType"),
                density = json.getNullableString("density"),
                vendorID = json.getNullableString("vendorID"),
                osNumber = json.getNullableString("osNumber"),
                resolution = json.getNullableString("resolution"),
            )
        }
    }

    override fun asJson(): JSONObject {
        if (!::json.isInitialized) {
            val registerJSON = JSONObject().apply {
                put("osName", osName)
                put("pushToken", pushToken)
                put("appVersion", appVersion)
                put("clientVersion", clientVersion)
                put("locale", locale)
                put("timeZone", timeZone)
                put("hardwareType", hardwareType)
                put("density", density)
                put("vendorID", vendorID)
                put("osNumber", osNumber)
                put("resolution", resolution)
            }

            json = JSONObject().apply {
                put("register", registerJSON)
            }
        }
        return json
    }

    override fun asString(): String {
        return asJson().toString()
    }
}