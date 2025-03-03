package com.appoxee.internal.ui.inapp.web

import android.app.Activity
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.Companion.PRIVATE
import com.appoxee.internal.container.ActionContainer
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.inapp.InappType
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.TrackingParams
import com.appoxee.internal.ui.inapp.InappActionHandler
import com.appoxee.internal.ui.inapp.Template
import com.appoxee.internal.util.DispatchersProvider
import com.appoxee.internal.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

internal class WebFactory(
    private val scope: CoroutineScope,
    private val dispatchersProvider: DispatchersProvider,
    private val actionContainer: ActionContainer,
) {
    private val TAG = this::class.java.name
    private var job: Job? = null

    fun <T : Message> createBanner(
        context: Activity,
        message: T,
        onShow: ((T) -> Unit)? = null,
        onMessageClosed: ((T, TrackingKey, TrackingParams) -> Unit)? = null
    ) {
        val inappActionHandler = actionContainer.inappActionHandler
        val delaySeconds = getDelay(message)
        var template: Template
        job = scope.launch {
            Logger.d(TAG, "createBanner: ${message.type.name}")
            delay(TimeUnit.SECONDS.toMillis(delaySeconds))
            withContext(dispatchersProvider.mainDispatcher) {
                template = createTemplate(context, inappActionHandler, message, onMessageClosed)
                template.show()
                onShow?.invoke(message)
            }
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    fun <T : Message> createTemplate(
        context: Activity,
        actionHandler: InappActionHandler,
        message: T,
        onMessageClosed: ((T, TrackingKey, TrackingParams) -> Unit)? = null
    ): Template {
        return when (message.type) {
            InappType.FULLSCREEN -> {
                FullscreenWebTemplate(
                    context,
                    actionHandler,
                    message,
                    scope,
                    dispatchersProvider,
                    onMessageClosed
                )
            }

            InappType.BANNER -> {
                BannerWebTemplate(
                    context,
                    actionHandler,
                    message,
                    scope,
                    dispatchersProvider,
                    onMessageClosed
                )
            }

            InappType.DIALOG -> {
                StandardWebTemplate(
                    context,
                    actionHandler,
                    message,
                    scope,
                    dispatchersProvider,
                    onMessageClosed
                )
            }
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    fun getDelay(message: Message): Long {
        val seconds = (message.behaviour?.delaySeconds ?: 0).toLong()
        return TimeUnit.SECONDS.toMillis(seconds)
    }
}