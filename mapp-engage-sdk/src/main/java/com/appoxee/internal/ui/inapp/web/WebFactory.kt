package com.appoxee.internal.ui.inapp.web

import android.app.Activity
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.inapp.InappType
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.TrackingParams
import com.appoxee.internal.model.response.inapp.WebInappMessage
import com.appoxee.internal.ui.inapp.InappActionHandlerImpl
import com.appoxee.internal.ui.inapp.Template
import com.appoxee.internal.util.Dispatchers
import com.appoxee.internal.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

internal class WebFactory(
    private val scope: CoroutineScope,
    private val dispatchers: Dispatchers,
) {
    private val TAG = this::class.java.name
    private var job: Job? = null

    fun <T : Message> createBanner(
        context: Activity,
        message: T,
        onShow: ((T) -> Unit)? = null,
        onMessageClosed: ((T, TrackingKey, TrackingParams) -> Unit)? = null
    ) {
        val delaySeconds = message.behaviour?.delaySeconds?.toLong() ?: 0
        val actionHandler = InappActionHandlerImpl(context)
        val webMessage = (message as? WebInappMessage) ?: return
        var template: Template
        job = scope.launch {
            Logger.d(TAG, "createBanner: ${message.type.name}")
            delay(TimeUnit.SECONDS.toMillis(delaySeconds))
            withContext(dispatchers.mainDispatcher) {
                when (webMessage.type) {
                    InappType.FULLSCREEN -> {
                        template = FullscreenWebTemplate(
                            context,
                            actionHandler,
                            message,
                            scope,
                            dispatchers,
                            onMessageClosed
                        )
                    }

                    InappType.BANNER -> {
                        template =
                            BannerWebTemplate(
                                context,
                                actionHandler,
                                message,
                                scope,
                                dispatchers,
                                onMessageClosed
                            )
                    }

                    InappType.DIALOG -> {
                        template = StandardWebTemplate(
                            context,
                            actionHandler,
                            message,
                            scope,
                            dispatchers,
                            onMessageClosed
                        )
                    }
                }
                template.show()
                onShow?.invoke(message)
            }
        }
    }
}