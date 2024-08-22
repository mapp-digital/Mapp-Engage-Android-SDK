package com.appoxee.internal.model.request.events

/**
 * Defines possible event types of a single notification
 * Every click on some notification UI part produces some event type
 */
internal enum class EventType {
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
    BUTTON3;

    fun toAction(): String {
        return when (this.ordinal) {
            CLICK.ordinal -> "CLICK"
            DISMISS.ordinal -> "DISMISS"
            BUTTON1.ordinal -> "BUTTON1"
            BUTTON2.ordinal -> "BUTTON2"
            BUTTON3.ordinal -> "BUTTON3"
            else -> ""
        }
    }
}