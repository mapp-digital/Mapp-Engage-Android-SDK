package com.appoxee.internal.model.request

import com.appoxee.internal.network.NetworkData
import com.appoxee.internal.util.getNullableString
import kotlinx.parcelize.IgnoredOnParcel
import org.json.JSONObject

internal data class RegisterDevice(
    val osName: String? = "N/A",
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

    @IgnoredOnParcel
    private lateinit var json: JSONObject

    companion object {
        fun fromJSON(json: JSONObject): RegisterDevice {
            return RegisterDevice(
                osName = json.getNullableString("osName"),
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

    fun getChangedParams(other: RegisterDevice?): Map<String, String> {
        val changedParams = mutableMapOf<String, String>()

        if (other == null) return changedParams

        if (osName != other.osName)
            changedParams["osName"] = other.osName ?: ""

        if (appVersion != other.appVersion)
            changedParams["appVersion"] = other.appVersion ?: ""

        if (clientVersion != other.clientVersion)
            changedParams["clientVersion"] = other.clientVersion ?: ""

        if (locale != other.locale)
            changedParams["locale"] = other.locale ?: ""

        if (timeZone != other.timeZone)
            changedParams["timeZone"] = other.timeZone ?: ""

        if (hardwareType != other.hardwareType)
            changedParams["hardwareType"] = other.hardwareType ?: ""

        if (density != other.density)
            changedParams["density"] = other.density ?: ""

        if (vendorID != other.vendorID)
            changedParams["vendorID"] = other.vendorID ?: ""

        if (osNumber != other.osNumber)
            changedParams["osNumber"] = other.osNumber ?: ""

        if (resolution != other.resolution)
            changedParams["resolution"] = other.resolution ?: ""

        return changedParams.filter { it.value.isNotEmpty() }.toMap()
    }
}