package com.appoxee.internal.ui.inapp

import com.appoxee.internal.model.response.inapp.InappButton

interface ActionHandler {
    fun handleAction(inappButton: InappButton)

    fun handleDeeplink(inappButton: InappButton)

    fun handleAppStore(inappButton: InappButton)

    fun handleLandingPageInApp(inappButton: InappButton)

    fun handleLandingPageExternal(inappButton: InappButton)

    fun handleDialer(inappButton: InappButton)
}