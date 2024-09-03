package com.appoxee.internal.model.response.inapp

enum class InappType(val value: Int) {
    FULLSCREEN(0),
    BANNER(1),
    DIALOG(2);

    companion object {
        fun from(type: Int): InappType = when (type) {
            1 -> BANNER
            2 -> DIALOG
            else -> FULLSCREEN
        }

    }
}