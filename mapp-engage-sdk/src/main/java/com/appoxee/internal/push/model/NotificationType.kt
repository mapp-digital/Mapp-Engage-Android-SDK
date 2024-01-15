package com.appoxee.internal.push.model

internal enum class NotificationType(val value: String) {
    TEXT("text"),
    GIF("gif"),
    IMAGE("image"),
    VIDEO("video");

    override fun toString(): String {
        return value
    }

    companion object {
        fun fromString(value: String?): NotificationType {
            return when (value) {
                IMAGE.value -> IMAGE
                GIF.value -> GIF
                VIDEO.value -> VIDEO
                else -> TEXT
            }
        }
    }
}