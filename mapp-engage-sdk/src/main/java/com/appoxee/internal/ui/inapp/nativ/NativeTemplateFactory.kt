package com.appoxee.internal.ui.inapp.nativ

import android.annotation.SuppressLint
import android.app.Activity
import com.appoxee.internal.model.response.inapp.InappType
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.ui.inapp.ActionHandlerImpl
import com.appoxee.internal.ui.inapp.Template
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

internal class NativeTemplateFactory(private val scope: CoroutineScope) {
    private var job: Job? = null

    @SuppressLint("SourceLockedOrientationActivity")
    fun <T : Message> createBanner(
        context: Activity,
        message: T,
        onMessageClosed: ((T) -> Unit)? = null
    ) {
        val delaySeconds = message.behaviour?.delaySeconds?.toLong() ?: 0
        val actionHandler = ActionHandlerImpl(context)
        var template: Template
        job = scope.launch {
            delay(TimeUnit.SECONDS.toMillis(delaySeconds))
            withContext(Dispatchers.Main) {
                if (context.isDestroyed) return@withContext
                template = when (message.type) {
                    InappType.FULLSCREEN -> {
                        FullscreenNativeTemplate(
                            context,
                            actionHandler,
                            message,
                            scope,
                            onMessageClosed
                        )
                    }

                    InappType.BANNER -> {
                        BannerNativeTemplate(
                            context,
                            actionHandler,
                            message,
                            scope,
                            onMessageClosed
                        )
                    }

                    InappType.DIALOG -> {
                        StandardNativeTemplate(
                            context,
                            actionHandler,
                            message,
                            scope,
                            onMessageClosed
                        )
                    }
                }

                template.show()
            }
        }
    }
}