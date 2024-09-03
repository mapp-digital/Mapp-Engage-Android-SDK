package com.appoxee.internal.ui.inapp.web

import android.app.Activity
import android.content.Context
import com.appoxee.internal.model.response.inapp.InappType
import com.appoxee.internal.model.response.inapp.WebInappMessage
import kotlinx.coroutines.CoroutineScope

internal class WebTemplateFactory(private val scope: CoroutineScope) {
    fun createBanner(context: Activity, message: WebInappMessage) {
        when (message.type) {
            InappType.FULLSCREEN -> {
                createFullscreenInapp(context, message)
            }

            InappType.BANNER -> {
                createBannerInapp(context, message)
            }

            InappType.DIALOG -> {
                createModalInapp(context, message)
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