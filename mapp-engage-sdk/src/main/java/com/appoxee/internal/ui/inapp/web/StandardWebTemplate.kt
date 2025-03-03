package com.appoxee.internal.ui.inapp.web

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.appoxee.R
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.TrackingParams
import com.appoxee.internal.model.response.inapp.WebInappMessage
import com.appoxee.internal.ui.custom.MappWebView
import com.appoxee.internal.ui.inapp.InappActionHandler
import com.appoxee.internal.ui.inapp.Template
import com.appoxee.internal.util.DispatchersProvider
import com.appoxee.internal.util.LibraryExtensions.getDisplayMetrics
import com.appoxee.internal.util.LibraryExtensions.toPx
import com.appoxee.internal.util.Logger
import kotlinx.coroutines.CoroutineScope

internal class StandardWebTemplate<T : Message>(
    private val activity: Activity,
    inappActionHandler: InappActionHandler,
    private val message: T,
    scope: CoroutineScope,
    dispatchersProvider: DispatchersProvider,
    private val onMessageClosed: ((T, TrackingKey, TrackingParams) -> Unit)? = null
) : Template(inappActionHandler, scope, dispatchersProvider) {
    private lateinit var alertDialog: AlertDialog
    private var height: Int = 0
    private var width: Int = 0
    private var webView: MappWebView? = null

    init {
        createTemplate()
    }

    private fun createTemplate() {
        val layoutRes = R.layout.me_inapp_web_template
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(layoutRes, null)

        width = (activity.getDisplayMetrics().widthPixels * ((message.location?.width
            ?: 100) / 100f)).toInt()
        height = (activity.getDisplayMetrics().heightPixels * ((message.location?.height
            ?: 100) / 100f)).toInt()
        alertDialog = AlertDialog.Builder(activity).create().apply {
            onViewCreated(message, view) { onDismiss() }
            setupViews(activity, view, (message as WebInappMessage))
            setView(view)
            setCancelable(false)
            setOnDismissListener {
                webView?.setOnButtonClick(null)
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                onMessageClosed?.invoke(message, trackingKeyResult, trackingParams)
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity", "SetJavaScriptEnabled")
    private fun setupViews(
        activity: Activity, view: View, message: WebInappMessage
    ) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        webView = MappWebView.getInstance(activity.applicationContext).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
        }
        webView?.also { webView ->
            webView.setOnButtonClick { actionData ->
                handleWebButton(actionData)
                onDismiss()
            }
            webView.setBackgroundColor(Color.LTGRAY)
            (message as? WebInappMessage)?.content?.let { html ->
                Logger.d(TAG, "HTML: $html")
                webView.loadData(html)
            }
        }

        (view as? ViewGroup)?.addView(webView, 0)
    }

    private fun onDismiss() {
        alertDialog.dismiss()
        job?.cancel()
    }

    override fun show() {
        alertDialog.show()
        alertDialog.window?.let {
            it.setWindowAnimations(R.style.CustomLeftDialogAnimation)
            it.setLayout(width, height)
            it.setBackgroundDrawable(GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = activity.toPx(dialogRadius).toFloat()
            })
        }
    }
}