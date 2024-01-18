package com.appoxee.internal.model.request.events

/**
 * Defines click types that can be executed from a notification
 * of a Mapp's push message
 */
internal enum class ClickType(val value: String) {
    /**
     * open launching activity of a host application
     */
    LAUNCH_APP("LAUNCH_APP"),

    /**
     * open url in a browser
     */
    OPEN_LANDING_PAGE("OPEN_LANDING_PAGE"),

    /**
     * delegate url to a registered activity
     */
    OPEN_DEEP_LINK("OPEN_DEEP_LINK"),

    /**
     * open Google Play Store's url (some application page)
     */
    OPEN_STORE("OPEN_PLAY_STORE"),

    /**
     * open system dialer
     */
    OPEN_DIALER("OPEN_DIALER"),

    /**
     * dismiss notification
     */
    DISMISS("DISMISS"),


    /**
     * open rich push message (message with an image/video)
     */
    OPEN_RICH_PUSH("OPEN_RICH_PUSH");


    companion object {
        fun fromString(value: String?): ClickType {
            return when (value) {
                LAUNCH_APP.value -> LAUNCH_APP
                OPEN_LANDING_PAGE.value -> OPEN_LANDING_PAGE
                OPEN_DEEP_LINK.value -> OPEN_DEEP_LINK
                OPEN_STORE.value -> OPEN_STORE
                OPEN_DIALER.value -> OPEN_DIALER
                OPEN_RICH_PUSH.value -> OPEN_RICH_PUSH
                else -> DISMISS
            }
        }

        /**
         * Backend can only accept values from 0-4 (included),
         * so any other defined clickType must be omitted from sending
         */
        fun ClickType.numeric(): Int? {
            return if (this.ordinal <= 4) this.ordinal else null
        }
    }
}