package com.appoxee.internal.model.response.inapp

import androidx.media3.extractor.mp4.Track
import com.appoxee.internal.model.request.events.TrackingKey

enum class InappActionType(private val value: String) {
    DEEPLINK("0"),
    LANDING_PAGE("1"),
    APP_STORE("2"),
    DIALER("3");

    companion object {
        fun from(type: String?): InappActionType? = when (type) {
            "0" -> DEEPLINK
            "1" -> LANDING_PAGE
            "2" -> APP_STORE
            "3" -> DIALER
            else -> null
        }

        fun fromAction(action: String?): InappActionType? = when (action) {
            "dialer" -> DIALER
            "landingPage" -> LANDING_PAGE
            "appStore" -> APP_STORE
            "deepLink" -> DEEPLINK
            else -> null
        }
    }
}