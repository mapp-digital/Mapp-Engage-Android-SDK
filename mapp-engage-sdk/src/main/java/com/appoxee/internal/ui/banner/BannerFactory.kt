package com.appoxee.internal.ui.banner

import android.app.Activity
import android.content.Context
import androidx.annotation.LayoutRes
import com.appoxee.R
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.NativeInappMessage
import com.appoxee.internal.model.response.inapp.WebInappMessage

internal val FULLSCREEN_TYPE_INAPP = 0
internal val BANNER_TYPE_INAPP = 1
internal val MODAL_TYPE_INAPP = 2

internal abstract class BannerFactory {
    companion object {
        @JvmStatic
        fun <T : Message> createBanner(
            context: Activity,
            message: T,
            onMessageClosed: (Message) -> Unit
        ) {
            when (message) {
                is NativeInappMessage -> {
                    val nativeBannerFactory = NativeBannerFactory()
                    nativeBannerFactory.createBanner(context, message, onMessageClosed)
                }

                is WebInappMessage -> {
                    val webBannerFactory = WebBannerFactory()
                    webBannerFactory.createBanner(context, message)
                }

                else -> {
                    null
                }
            }
        }
    }
}