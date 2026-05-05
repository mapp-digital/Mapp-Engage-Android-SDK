package com.appoxee.internal.model.response.inapp

enum class InappActionType(private val value: String) {
    DEEPLINK("0"),
    LANDING_PAGE("1"),
    APP_STORE("2"),
    DIALER("3"),
    CUSTOM("4");

    companion object {
        fun from(type: String?): InappActionType? = when (type) {
            "0" -> DEEPLINK
            "1" -> LANDING_PAGE
            "2" -> APP_STORE
            "3" -> DIALER
            "4" -> CUSTOM
            else -> null
        }

        fun fromAction(action: String?): InappActionType? = when (action) {
            "dialer" -> DIALER
            "landingPage" -> LANDING_PAGE
            "appStore" -> APP_STORE
            "deepLink" -> DEEPLINK
            "custom" -> CUSTOM
            else -> null
        }
    }
}