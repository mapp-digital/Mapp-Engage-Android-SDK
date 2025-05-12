package com.appoxee.internal.ui.inapp.web

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import com.appoxee.sdk.R
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.inapp.BannerPosition
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.TrackingParams
import com.appoxee.internal.model.response.inapp.WebInappMessage
import com.appoxee.internal.ui.custom.MappWebView
import com.appoxee.internal.ui.inapp.InappActionHandler
import com.appoxee.internal.ui.inapp.Template
import com.appoxee.internal.util.DispatchersProvider
import com.appoxee.internal.util.LibraryExtensions.getDisplayMetrics
import com.appoxee.internal.util.Logger
import kotlinx.coroutines.CoroutineScope

internal class BannerWebTemplate<T : Message>(
    private val activity: Activity,
    inappActionHandler: InappActionHandler,
    private val message: T,
    scope: CoroutineScope,
    dispatchersProvider: DispatchersProvider,
    private val onMessageClosed: ((T, TrackingKey, TrackingParams) -> Unit)? = null
) : Template(inappActionHandler, scope, dispatchersProvider) {

    private lateinit var windowManager: WindowManager
    private lateinit var layoutParams: LayoutParams
    private lateinit var view: View
    private var webView: MappWebView? = null

    init {
        createTemplate()
    }

    private fun createTemplate() {
        val layoutRes = R.layout.me_inapp_web_banner
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val position = BannerPosition.fromValue(message.location?.bannerPosition?.position ?: 0)
        val width =
            (activity.getDisplayMetrics().widthPixels * ((message.location?.width ?: 100) / 100f))
                .toInt()
        val height =
            (activity.getDisplayMetrics().heightPixels * ((message.location?.height ?: 100) / 100f))
                .toInt()
        view = inflater.inflate(layoutRes, null)
        windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        layoutParams = LayoutParams(
            LayoutParams.TYPE_APPLICATION_SUB_PANEL
        ).also {
            it.width = width
            it.height = height
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

        onViewCreated(message, view) { onDismissed() }
        setupViews(activity, view, message)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setupViews(
        context: Activity,
        view: View,
        message: T
    ) {
        context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        webView = MappWebView.getInstance(activity.applicationContext).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.RED)
        }

        webView?.also { webView ->
            webView.setOnButtonClick { actionData ->
                handleWebButton(actionData)
                onDismissed()
            }

            webView.setBackgroundColor(Color.LTGRAY)
            (message as? WebInappMessage)?.content?.let { html ->
                Logger.d(TAG, "HTML: $html")
                webView.loadData(html)
            }
        }

        (view as? ViewGroup)?.addView(webView, 0)
    }

    override fun show() {
        windowManager.addView(view, layoutParams)
    }

    private fun onDismissed() {
        windowManager.removeViewImmediate(view)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        job?.cancel()
        onMessageClosed?.invoke(message, trackingKeyResult, trackingParams)
    }
}