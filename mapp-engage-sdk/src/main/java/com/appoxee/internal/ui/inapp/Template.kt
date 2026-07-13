package com.appoxee.internal.ui.inapp

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import com.appoxee.sdk.R
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.inapp.ActionData
import com.appoxee.internal.model.response.inapp.InappButton
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.TrackingParams
import com.appoxee.internal.util.DispatchersProvider
import com.appoxee.internal.util.LibraryExtensions.toColor
import com.appoxee.internal.util.LibraryExtensions.toPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

internal abstract class Template(
    private val inappActionHandler: InappActionHandler,
    private val scope: CoroutineScope,
    private val dispatchersProvider: DispatchersProvider
) {
    val TAG
        get() = this::class.java.name

    val buttonRadius: Int
        get() = 15
    val dialogRadius: Int
        get() = 20
    protected var job: Job? = null
    protected open var trackingKeyResult: TrackingKey = TrackingKey.IA_MSG_DISMISSED
    protected open val trackingParams = TrackingParams()
    private val startingTime: Long = System.currentTimeMillis()


    protected open fun handleWebButton(actionData: ActionData) {
        trackingParams.link = actionData.link
        trackingParams.timeSinceLastDisplay = System.currentTimeMillis() - startingTime
        trackingParams.reason = null
        trackingKeyResult = actionData.toTrackingKey()
        inappActionHandler.handleAction(actionData)
    }

    protected open fun handleNativeButton(
        button: Button,
        inappButton: InappButton,
        onDismiss: (() -> Unit)?
    ) {
        button.visibility = if (inappButton.text.isEmpty()) View.GONE else View.VISIBLE
        button.text = inappButton.text
        button.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = button.context.toPx(buttonRadius).toFloat()
        }
        button.backgroundTintList = ColorStateList.valueOf(inappButton.backgroundColor.toColor())
        button.setTextColor(inappButton.textColor.toColor())
        button.setOnClickListener {
            trackingParams.reason = null
            trackingParams.timeSinceLastDisplay = System.currentTimeMillis() - startingTime
            trackingParams.link = inappButton.link
            inappActionHandler.handleAction(inappButton.actionData)
            trackingKeyResult = inappButton.actionData.toTrackingKey()
            onDismiss?.invoke()
        }
    }

    protected open fun userDismissed(onDismiss: (() -> Unit)?) {
        trackingParams.timeSinceLastDisplay = System.currentTimeMillis() - startingTime
        trackingParams.reason = TrackingParams.REASON_USER_DISMISSED
        onDismiss?.invoke()
    }

    protected open fun expirationDismissed(onDismiss: (() -> Unit)?) {
        trackingParams.timeSinceLastDisplay = System.currentTimeMillis() - startingTime
        trackingParams.reason = TrackingParams.REASON_TIMEOUT_EXPIRATION
        onDismiss?.invoke()
    }

    protected open fun onViewCreated(message: Message, view: View, onDismiss: (() -> Unit)?) {
        view.findViewById<ImageButton>(R.id.ibClose)?.let {
            it.setOnClickListener {
                userDismissed {
                    onDismiss?.invoke()
                }
            }
        }

        message.behaviour?.displaySeconds?.takeIf { it > 0 }?.toLong()?.let { seconds ->
            job = scope.launch {
                delay(TimeUnit.SECONDS.toMillis(seconds))
                withContext(dispatchersProvider.mainDispatcher) {
                    expirationDismissed {
                        onDismiss?.invoke()
                    }
                }
            }
        }
    }

    abstract fun show()
}
