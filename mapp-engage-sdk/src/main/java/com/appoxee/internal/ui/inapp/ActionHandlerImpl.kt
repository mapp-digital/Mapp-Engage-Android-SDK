package com.appoxee.internal.ui.inapp

import android.content.Context
import android.widget.Toast
import com.appoxee.internal.model.response.inapp.ActionData
import com.appoxee.internal.model.response.inapp.InappActionType
import com.appoxee.internal.model.response.inapp.InappButton
import com.appoxee.internal.util.Logger

class ActionHandlerImpl(private val context: Context) : ActionHandler {
    private val TAG = this::class.java.name
    override fun handleAction(actionData: ActionData) {
        when (actionData.actionType) {
            InappActionType.DEEPLINK -> {
                handleDeeplink(actionData)
            }

            InappActionType.APP_STORE -> {
                handleAppStore(actionData)
            }

            InappActionType.LANDING_PAGE -> {
                if (actionData.openInApp) {
                    handleLandingPageInApp(actionData)
                } else {
                    handleLandingPageExternal(actionData)
                }
            }

            InappActionType.DIALER -> {
                handleDialer(actionData)
            }

            else -> {}
        }
    }

    override fun handleAction(button: InappButton) {
        handleAction(button.actionData)
    }

    override fun handleDeeplink(actionData: ActionData) {
        val message = "Deeplink: ${actionData.link}"
        Logger.d(TAG, message)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun handleAppStore(actionData: ActionData) {
        val message = "AppStore: ${actionData.link}"
        Logger.d(TAG, message)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun handleLandingPageInApp(actionData: ActionData) {
        val message = "Landing Page In App: ${actionData.link}"
        Logger.d(TAG, message)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun handleLandingPageExternal(actionData: ActionData) {
        val message = "Landing Page External: ${actionData.link}"
        Logger.d(TAG, message)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun handleDialer(actionData: ActionData) {
        val message = "Dialer: ${actionData.link}"
        Logger.d(TAG, message)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}