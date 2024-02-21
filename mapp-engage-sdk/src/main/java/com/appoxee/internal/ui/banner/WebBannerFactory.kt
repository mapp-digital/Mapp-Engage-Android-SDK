package com.appoxee.internal.ui.banner

import android.app.Activity
import android.content.Context
import com.appoxee.internal.model.response.inapp.WebInappMessage

internal class WebBannerFactory : BannerFactory() {
    fun createBanner(context: Activity, message: WebInappMessage) {
        when (message.type) {
            FULLSCREEN_TYPE_INAPP -> {
                createFullscreenInapp(context, message)
            }

            BANNER_TYPE_INAPP -> {
                createBannerInapp(context, message)
            }

            MODAL_TYPE_INAPP -> {
                createModalInapp(context, message)
            }

            else -> {
                null
            }
        }
    }

    private fun createFullscreenInapp(context: Context, message: WebInappMessage) {

    }

    private fun createBannerInapp(context: Context, message: WebInappMessage) {
    }

    private fun createModalInapp(context: Context, message: WebInappMessage) {

    }
}