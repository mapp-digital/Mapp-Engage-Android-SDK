package com.appoxee.internal.ui.inapp

import com.appoxee.internal.model.response.inapp.ActionData

internal fun interface InappActionHandler {
    fun handleAction(actionData: ActionData)
}