package com.appoxee.internal.ui.inapp.nativ

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.Button
import android.widget.ImageView
import android.widget.Space
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.Companion.PRIVATE
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Scale
import com.appoxee.R
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.inapp.BannerPosition
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.NativeInappMessage
import com.appoxee.internal.model.response.inapp.TrackingParams
import com.appoxee.internal.ui.inapp.InappActionHandler
import com.appoxee.internal.ui.inapp.Template
import com.appoxee.internal.util.Dispatchers
import com.appoxee.internal.util.LibExt.toColor
import kotlinx.coroutines.CoroutineScope

internal class BannerNativeTemplate<T : Message>(
    private val activity: Activity,
    inappActionHandler: InappActionHandler,
    private val message: T,
    scope: CoroutineScope,
    dispatchers: Dispatchers,
    private val onMessageClosed: ((T, TrackingKey, TrackingParams) -> Unit)? = null
) : Template(inappActionHandler, scope, dispatchers) {

    private lateinit var windowManager: WindowManager
    private lateinit var layoutParams: LayoutParams
    private lateinit var view: View

    init {
        createTemplate()
    }

    @VisibleForTesting(otherwise = PRIVATE)
    fun createTemplate() {
        val layoutRes = R.layout.me_inapp_banner_top_bottom
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val position = getPosition((message as NativeInappMessage))

        view = inflater.inflate(layoutRes, null)
        windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        layoutParams = LayoutParams(
            LayoutParams.TYPE_APPLICATION_SUB_PANEL
        ).also {
            it.width = LayoutParams.MATCH_PARENT
            it.height = LayoutParams.WRAP_CONTENT
            it.flags = (LayoutParams.FLAG_HARDWARE_ACCELERATED
                    or LayoutParams.FLAG_NOT_FOCUSABLE
                    or LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
            it.format = PixelFormat.TRANSLUCENT
            if (position == BannerPosition.BOTTOM) {
                it.gravity = Gravity.BOTTOM
                it.windowAnimations = R.style.CustomDownDialogAnimation
            } else {
                it.gravity = Gravity.TOP
                it.windowAnimations = R.style.CustomUpDialogAnimation
            }
        }

        onViewCreated(message, view) { onDismiss() }
        setupViews(activity, view, (message as NativeInappMessage))
        view.setBackgroundColor(message.templateBackgroundColor.toColor())
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setupViews(
        context: Activity,
        view: View,
        message: NativeInappMessage
    ) {
        context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        view.findViewById<TextView>(R.id.tvTitle)?.let {
            it.visibility = if (message.title.isEmpty()) View.GONE else View.VISIBLE
            it.text = message.title
            message.titleColor?.toColor()?.let { color -> it.setTextColor(color) }
        }

        view.findViewById<TextView>(R.id.tvContent)?.let {
            it.visibility = if (message.content.isEmpty()) View.INVISIBLE else View.VISIBLE
            it.text = message.content
            message.contentColor?.toColor()?.let { color -> it.setTextColor(color) }
        }

        view.findViewById<ImageView>(R.id.ivImage)?.let {
            it.visibility = if (message.imageUrl.isNullOrEmpty()) View.GONE else View.VISIBLE
            ImageRequest.Builder(context)
                .data(message.imageUrl)
                .target(it)
                .scale(Scale.FILL)
                .build()
                .also {
                    ImageLoader(context).enqueue(it)
                }
        }

        view.findViewById<Button>(R.id.btn1)?.let {
            message.buttons.getOrNull(0)?.let { btn ->
                handleNativeButton(it, btn) { onDismiss() }
            }
        }

        view.findViewById<Button>(R.id.btn2)?.let {
            message.buttons.getOrNull(1)?.let { btn ->
                handleNativeButton(it, btn) { onDismiss() }
            }
        }

        view.findViewById<Space>(R.id.btnSpacer)?.let {
            if (message.buttons.size == 2) it.visibility = View.VISIBLE else View.GONE
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    fun getPosition(message: NativeInappMessage): BannerPosition {
        return BannerPosition.fromValue(message.location?.bannerPosition?.position ?: 0)
    }

    override fun show() {
        windowManager.addView(view, layoutParams)
    }

    private fun onDismiss() {
        windowManager.removeViewImmediate(view)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        job?.cancel()
        onMessageClosed?.invoke(message, trackingKeyResult, trackingParams)
    }
}