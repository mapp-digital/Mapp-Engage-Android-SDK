package com.appoxee.internal.ui.inapp.web

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebSettings.LOAD_NO_CACHE
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.RelativeLayout
import com.appoxee.R
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.WebInappMessage
import com.appoxee.internal.ui.inapp.ActionHandler
import com.appoxee.internal.ui.inapp.Template
import com.appoxee.internal.util.Dispatchers
import com.appoxee.internal.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

internal class FullscreenWebTemplate<T : Message>(
    private val activity: Activity,
    private val actionHandler: ActionHandler,
    private val message: T,
    private val scope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val onMessageClosed: ((T) -> Unit)? = null
) : Template {
    private lateinit var alertDialog: AlertDialog
    private var job: Job? = null
    private var webView: WebView? = null

    init {
        createTemplate()
    }

    private fun createTemplate() {
        val layoutRes = R.layout.me_inapp_web_standard
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(layoutRes, null)

        alertDialog = AlertDialog.Builder(
            activity,
            android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen
        ).create().apply {
            setupViews(activity, view, (message as WebInappMessage)) {
                dismiss()
                job?.cancel()
            }
            setView(view)
            setCancelable(false)
            setOnDismissListener {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                onMessageClosed?.invoke(message)
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity", "SetJavaScriptEnabled")
    private fun setupViews(
        activity: Activity, view: View, message: WebInappMessage, onDismiss: (() -> Unit)? = null
    ) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        webView = WebView(activity.applicationContext).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
        }

        webView?.also { webView ->
            webView.settings.also {
                it.javaScriptEnabled = true
                it.cacheMode = LOAD_NO_CACHE
            }
            webView.setBackgroundColor(Color.LTGRAY)
            (message as? WebInappMessage)?.decodedHtml?.let { html ->
                Logger.d(TAG, "HTML: $html")
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        }

        view.findViewById<ImageButton>(R.id.ibClose)?.let {
            it.setOnClickListener { onDismiss?.invoke() }
        }

        message.behaviour?.displaySeconds?.toLong()?.let { seconds ->
            job = scope.launch {
                delay(TimeUnit.SECONDS.toMillis(seconds))
                withContext(dispatchers.mainDispatcher) {
                    onDismiss?.invoke()
                }
            }
        }
    }

    override fun show() {
        alertDialog.show()
        alertDialog.window?.setWindowAnimations(R.style.CustomLeftDialogAnimation)
    }

    private fun reportEvent() {

    }
}