package com.appoxee.internal.model.response.inapp

import com.appoxee.internal.util.getLongOrDefault
import org.json.JSONObject

data class Location(
    val bannerPosition: BannerPosition,
    val height: Int,
    val width: Int
) {
    companion object {
        private const val DEFAULT_SIZE_PERCENT = 100

        fun fromJSON(json: JSONObject): Location {
            return Location(
                bannerPosition = BannerPosition.fromValue(json.getLongOrDefault("position", 0).toInt()),
                height = json.getSizePercent("height"),
                width = json.getSizePercent("width")
            )
        }

        private fun JSONObject.getSizePercent(name: String): Int {
            return getLongOrDefault(name, DEFAULT_SIZE_PERCENT.toLong())
                .toInt()
                .takeIf { it > 0 } ?: DEFAULT_SIZE_PERCENT
        }
    }
}
