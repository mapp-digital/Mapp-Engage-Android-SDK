package com.appoxee.internal.ui.inapp

import android.net.Uri
import com.appoxee.internal.model.response.inapp.ActionData
import com.appoxee.internal.model.response.inapp.InappActionType
import com.appoxee.internal.ui.action.ActionHandler

internal class InappActionHandlerImpl(private val actionHandler: ActionHandler) :
    InappActionHandler {
    override fun handleAction(actionData: ActionData) {
        actionData.actionType?.let { actionType ->
            when (actionType) {
                InappActionType.DEEPLINK -> {
                    actionData.link?.let { url ->
                        actionHandler.openDeepLink(
                            url,
                            actionData.messageId.toString()
                        )
                    }
                }

                InappActionType.APP_STORE -> {
                    actionData.link?.let { url ->
                        actionHandler.openAppStore(url)
                    }
                }

                InappActionType.LANDING_PAGE -> {
                    actionData.link?.let { url ->
                        if (actionData.openInApp) {
                            actionHandler.openLandingPageInternal(url)
                        } else {
                            actionHandler.openLandingPageExternal(url)
                        }
                    }
                }

                InappActionType.DIALER -> {
                    actionData.link?.let { phoneNumber ->
                        actionHandler.openDialer(phoneNumber)
                    }
                }

                InappActionType.CUSTOM -> {
                    actionData.link?.let { url ->
                        actionHandler.customAction(Uri.parse(url))
                    }
                }
            }
        }
    }
}