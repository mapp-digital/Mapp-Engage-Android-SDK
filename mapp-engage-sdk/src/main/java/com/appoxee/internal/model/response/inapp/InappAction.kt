package com.appoxee.internal.model.response.inapp

enum class InappAction(value: String) {
    DEEPLINK("0"),
    LANDING_PAGE("1"),
    APP_STORE("2"),
    DIALER("3");

    companion object {
        fun from(type: String?): InappAction? = when (type) {
            "0" -> DEEPLINK
            "1" -> LANDING_PAGE
            "2" -> APP_STORE
            "3" -> DIALER
            else -> null
        }
    }
}