package com.appoxee.internal.ui.banner

enum class BannerPosition(val position: Int) {
    TOP(0),
    BOTTOM(1);

    companion object {
        fun fromValue(value: Int): BannerPosition {
            return when (value) {
                0 -> TOP
                else -> BOTTOM
            }
        }
    }
}