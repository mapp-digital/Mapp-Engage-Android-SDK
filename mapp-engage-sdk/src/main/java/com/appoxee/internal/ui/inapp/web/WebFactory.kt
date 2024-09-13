package com.appoxee.internal.ui.inapp.web

import android.app.Activity
import com.appoxee.internal.model.response.inapp.InappType
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.WebInappMessage
import com.appoxee.internal.ui.inapp.ActionHandlerImpl
import com.appoxee.internal.ui.inapp.Template
import com.appoxee.internal.util.Dispatchers
import com.appoxee.internal.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

internal class WebFactory(private val scope: CoroutineScope, private val dispatchers: Dispatchers) {
    private val TAG = this::class.java.name
    private var job: Job? = null

    fun <T : Message> createBanner(
        context: Activity,
        message: T,
        onMessageClosed: ((T) -> Unit)? = null
    ) {
        val delaySeconds = message.behaviour?.delaySeconds?.toLong() ?: 0
        val actionHandler = ActionHandlerImpl(context)
        var template: Template?=null
        job = scope.launch {
            Logger.d(TAG, "createBanner: ${message.type.name}")
            delay(TimeUnit.SECONDS.toMillis(delaySeconds))
            withContext(dispatchers.mainDispatcher) {
                when ((message as WebInappMessage).type) {
                    InappType.FULLSCREEN -> {
                        null
                    }

                    InappType.BANNER -> {
                        null
                    }

                    InappType.DIALOG -> {
                        null
                    }
                }
                template?.show()
            }
        }
    }
}