package com.appoxee.internal.ui.action

import android.net.Uri
import com.appoxee.internal.ui.push.model.PushData

/**
 * Defines action which can be executed from push or inapp messages
 */
internal interface ActionHandler {
    fun openAppStore(url: String)
    fun openDeepLink(url: String, messageId: String?)
    fun openDialer(phoneNumber: String)
    fun openLandingPageExternal(url: String)
    fun openLandingPageInternal(url: String)
    fun openLaunchActivity()
    fun showGif(pushData: PushData)
    fun customAction(uri:Uri)
}