package com.appoxee.internal.ui.inapp.nativ

import android.annotation.SuppressLint
import android.app.Activity
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.Companion.PRIVATE
import com.appoxee.internal.container.ActionContainer
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.inapp.ContentTemplates
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.NativeInappMessage
import com.appoxee.internal.model.response.inapp.TrackingParams
import com.appoxee.internal.ui.inapp.InappActionHandler
import com.appoxee.internal.ui.inapp.Template
import com.appoxee.internal.util.DispatchersProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

internal class NativeFactory(
    private val scope: CoroutineScope,
    private val dispatchersProvider: DispatchersProvider,
    private val actionContainer: ActionContainer,
) {
    private var job: Job? = null

    @SuppressLint("SourceLockedOrientationActivity")
    fun <T : Message> createBanner(
        context: Activity,
        message: T,
        onShow: ((T) -> Unit)? = null,
        onMessageClosed: ((T, TrackingKey, TrackingParams) -> Unit)? = null
    ) {
        val inappActionHandler = actionContainer.inappActionHandler
        val delayMillis = getDelay(message)
        var template: Template
        job = scope.launch {
            delay(delayMillis)
            withContext(dispatchersProvider.mainDispatcher) {
                if (context.isDestroyed) return@withContext
                template = createTemplate(
                    context,
                    inappActionHandler,
                    message,
                    onMessageClosed
                )
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
        return when ((message as NativeInappMessage).contentTemplateId) {
            ContentTemplates.FULLSCREEN -> {
                FullscreenNativeTemplate(
                    context, actionHandler, message, scope, dispatchersProvider, onMessageClosed
                )
            }

            ContentTemplates.BANNER_BOTTOM, ContentTemplates.BANNER_TOP -> {
                BannerNativeTemplate(
                    context, actionHandler, message, scope, dispatchersProvider, onMessageClosed
                )
            }

            ContentTemplates.STANDARD -> {
                StandardNativeTemplate(
                    context, actionHandler, message, scope, dispatchersProvider, onMessageClosed
                )
            }

            ContentTemplates.BACKGROUND_IMAGE_FULLSCREEN -> {
                FullscreenImageNativeTemplate(
                    context, actionHandler, message, scope, dispatchersProvider, onMessageClosed
                )
            }

            ContentTemplates.BACKGROUND_IMAGE_STANDARD -> {
                StandardImageNativeTemplate(
                    context, actionHandler, message, scope, dispatchersProvider, onMessageClosed
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
