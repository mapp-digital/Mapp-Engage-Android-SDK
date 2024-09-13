package com.appoxee.internal.model.response.inapp

enum class ContentTemplates(val template: String) {
    STANDARD("standard"),
    BANNER_BOTTOM("banner bottom-banner"),
    BANNER_TOP("banner"),
    BACKGROUND_IMAGE_FULLSCREEN("full-screen background-img"),
    BACKGROUND_IMAGE_STANDARD("background-img"),
    FULLSCREEN("full-screen");

    companion object {
        fun from(template: String): ContentTemplates {
            return when (template) {
                STANDARD.template -> STANDARD
                BANNER_BOTTOM.template -> BANNER_BOTTOM
                BANNER_TOP.template -> BANNER_TOP
                BACKGROUND_IMAGE_FULLSCREEN.template -> BACKGROUND_IMAGE_FULLSCREEN
                BACKGROUND_IMAGE_STANDARD.template -> BACKGROUND_IMAGE_STANDARD
                else -> FULLSCREEN
            }
        }
    }
}