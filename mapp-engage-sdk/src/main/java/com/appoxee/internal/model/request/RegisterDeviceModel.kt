package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import com.appoxee.internal.util.getNonNullString
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

    companion object {
        fun fromJSON(json: JSONObject): RegisterDeviceModel {
            return RegisterDeviceModel(
                osName = json.getNonNullString("osName"),
                pushToken = json.getNonNullString("pushToken"),
                appVersion = json.getNonNullString("appVersion"),
                clientVersion = json.getNonNullString("clientVersion"),
                locale = json.getNonNullString("locale"),
                timeZone = json.getNonNullString("timeZone"),
                hardwareType = json.getNonNullString("hardwareType"),
                density = json.getNonNullString("density"),
                vendorID = json.getNonNullString("vendorID"),
                osNumber = json.getNonNullString("osNumber"),
                resolution = json.getNonNullString("resolution"),
            )

        }
    }

    override fun asJson(): JSONObject {
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

        val register = JSONObject().apply {
            put("register", registerJSON)
        }
        return register
    }

    override fun asString(): String {
        return asJson().toString()
    }
}