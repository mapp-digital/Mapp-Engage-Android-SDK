package com.appoxee.internal.model.response.inapp

enum class BackgroundType(val value: Int) {
    HALF_BACKGROUND(0),
    FULL_BACKGROUND(1);

    companion object {
        fun from(type: Int): BackgroundType = when (type) {
            0 -> HALF_BACKGROUND
            else -> FULL_BACKGROUND
        }
    }
}