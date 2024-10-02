package com.appoxee.internal.ui.inapp.nativ

import android.annotation.SuppressLint
import android.app.Activity
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.Companion.PRIVATE
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.inapp.ContentTemplates
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.NativeInappMessage
import com.appoxee.internal.model.response.inapp.TrackingParams
import com.appoxee.internal.ui.inapp.InappActionHandler
import com.appoxee.internal.ui.inapp.InappActionHandlerImpl
import com.appoxee.internal.ui.inapp.Template
import com.appoxee.internal.util.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

internal class NativeFactory(
    private val scope: CoroutineScope,
    private val dispatchers: Dispatchers,
) {
    private var job: Job? = null

    @SuppressLint("SourceLockedOrientationActivity")
    fun <T : Message> createBanner(
        context: Activity,
        message: T,
        onShow: ((T) -> Unit)? = null,
        onMessageClosed: ((T, TrackingKey, TrackingParams) -> Unit)? = null
    ) {
        val actionHandler = getActionHandler(context)
        val delaySeconds = getDelay(message)
        var template: Template
        job = scope.launch {
            delay(TimeUnit.SECONDS.toMillis(delaySeconds))
            withContext(dispatchers.mainDispatcher) {
                if (context.isDestroyed) return@withContext
                template = createTemplate(
                    context,
                    actionHandler,
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
                    context, actionHandler, message, scope, dispatchers, onMessageClosed
                )
            }

            ContentTemplates.BANNER_BOTTOM, ContentTemplates.BANNER_TOP -> {
                BannerNativeTemplate(
                    context, actionHandler, message, scope, dispatchers, onMessageClosed
                )
            }

            ContentTemplates.STANDARD -> {
                StandardNativeTemplate(
                    context, actionHandler, message, scope, dispatchers, onMessageClosed
                )
            }

            ContentTemplates.BACKGROUND_IMAGE_FULLSCREEN -> {
                FullscreenImageNativeTemplate(
                    context, actionHandler, message, scope, dispatchers, onMessageClosed
                )
            }

            ContentTemplates.BACKGROUND_IMAGE_STANDARD -> {
                StandardImageNativeTemplate(
                    context, actionHandler, message, scope, dispatchers, onMessageClosed
                )
            }
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    fun getActionHandler(context: Activity): InappActionHandler = InappActionHandlerImpl(context)

    @VisibleForTesting(otherwise = PRIVATE)
    fun getDelay(message: Message): Long {
        val seconds = (message.behaviour?.delaySeconds ?: 0).toLong()
        return TimeUnit.SECONDS.toMillis(seconds)
    }
}