package com.appoxee.internal.ui.inapp

import com.appoxee.internal.model.response.inapp.ActionData
import com.appoxee.internal.model.response.inapp.InappButton

interface InappActionHandler {
    fun handleAction(actionData: ActionData)

    fun handleDeeplink(actionData: ActionData)

    fun handleAppStore(actionData: ActionData)

    fun handleLandingPageInApp(actionData: ActionData)

    fun handleLandingPageExternal(actionData: ActionData)

    fun handleDialer(actionData: ActionData)
}