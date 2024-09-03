package com.appoxee.internal.ui.inapp

import android.content.Context
import android.widget.Toast
import com.appoxee.internal.model.response.inapp.InappAction
import com.appoxee.internal.model.response.inapp.InappButton
import com.appoxee.internal.util.Logger

class ActionHandlerImpl(private val context: Context) : ActionHandler {
    private val TAG = this::class.java.name
    override fun handleAction(inappButton: InappButton) {
        when (inappButton.action) {
            InappAction.DEEPLINK -> {
                handleDeeplink(inappButton)
            }

            InappAction.APP_STORE -> {
                handleAppStore(inappButton)
            }

            InappAction.LANDING_PAGE -> {
                if (inappButton.openInApp) {
                    handleLandingPageInApp(inappButton)
                } else {
                    handleLandingPageExternal(inappButton)
                }
            }

            InappAction.DIALER -> {
                handleDialer(inappButton)
            }

            else -> {}
        }
    }

    override fun handleDeeplink(inappButton: InappButton) {
        val message = "Deeplink: ${inappButton.link}"
        Logger.d(TAG, message)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun handleAppStore(inappButton: InappButton) {
        val message = "AppStore: ${inappButton.link}"
        Logger.d(TAG, message)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun handleLandingPageInApp(inappButton: InappButton) {
        val message = "Landing Page In App: ${inappButton.link}"
        Logger.d(TAG, message)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun handleLandingPageExternal(inappButton: InappButton) {
        val message = "Landing Page External: ${inappButton.link}"
        Logger.d(TAG, message)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun handleDialer(inappButton: InappButton) {
        val message = "Dialer: ${inappButton.link}"
        Logger.d(TAG, message)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}