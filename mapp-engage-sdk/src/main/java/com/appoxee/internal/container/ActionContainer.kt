package com.appoxee.internal.container

import android.content.Context
import com.appoxee.internal.ui.action.ActionHandler
import com.appoxee.internal.ui.action.MessageActionHandler
import com.appoxee.internal.ui.inapp.InappActionHandlerImpl

internal class ActionContainer(context: Context) {
    val actionHandler: ActionHandler by lazy { MessageActionHandler(context) }

    val inappActionHandler: InappActionHandlerImpl by lazy {
        InappActionHandlerImpl(actionHandler)
    }
}