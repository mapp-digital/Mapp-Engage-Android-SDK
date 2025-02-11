package com.appoxee.internal.ui.inapp

import com.appoxee.internal.model.response.inapp.ActionData
import com.appoxee.internal.model.response.inapp.InappButton

interface InappActionHandler {
    fun handleAction(actionData: ActionData)
}