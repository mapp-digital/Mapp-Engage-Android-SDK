package com.appoxee.internal.model.request.events

/**
 * Defines possible click types of a single notification
 */
internal enum class NotificationClick {
    /**
     * click on notification body
     */
    CLICK,

    /**
     * dismiss with swipe or closing with group of notifications
     */
    DISMISS,

    /**
     * clicked 1st notification action button
     */
    BUTTON1,

    /**
     * clicked 2nd notification action button
     */
    BUTTON2,

    /**
     * clicked 3rd notification action button
     */
    BUTTON3,
}