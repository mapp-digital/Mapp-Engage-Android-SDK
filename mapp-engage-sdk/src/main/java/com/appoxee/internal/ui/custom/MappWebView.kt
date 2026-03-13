package com.appoxee.internal.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings.LOAD_NO_CACHE
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.appoxee.internal.model.response.inapp.ActionData
import com.appoxee.internal.model.response.inapp.InappActionType
import com.appoxee.internal.util.Logger
import okhttp3.internal.toLongOrDefault

@SuppressLint("SetJavaScriptEnabled")
class MappWebView private constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyle: Int,
    defStyleRes: Int
) :
    FrameLayout(context, attrs, defStyle, defStyleRes) {
    private val TAG = this::class.java.name

    private var onButtonClick: ((ActionData) -> Unit)? = null

    private constructor(context: Context) : this(context, null, 0, 0)

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private lateinit var instance: MappWebView

        @JvmStatic
        @Synchronized
        internal fun getInstance(context: Context): MappWebView {
            if (!::instance.isInitialized) {
                instance = MappWebView(context.applicationContext)
                instance.loadData("about:blank")
                instance.webView.let {
                    it.stopLoading()
                    it.clearCache(true)
                }
            } else {
                (instance.parent as? ViewGroup)?.let {
                    it.removeView(instance)
                }
            }
            return instance
        }
    }

    private val webView: WebView

    private var progressBar: ProgressBar = ProgressBar(context.applicationContext).apply {
        isIndeterminate = true
    }

    private val mappChromeClient = object : WebChromeClient() {
        private val TAG = MappWebView::class.java.name
        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            consoleMessage?.let {
                Logger.d(TAG, "${it.lineNumber()} - ${it.message()}")
            }
            return super.onConsoleMessage(consoleMessage)
        }
    }

    private val mappWebClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            progressBar.visibility = VISIBLE
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            progressBar.visibility = GONE
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            request?.url?.let { uri ->
                val action = uri.host
                val scheme = uri.scheme
                val link = uri.getQueryParameter("link")
                val messageId=uri.getQueryParameter("messageId")?.toLongOrDefault(-1) ?: -1
                val openInApp = uri.getQueryParameter("openInApp")?.toInt() == 1
                onButtonClick?.invoke(
                    ActionData(
                        link = link,
                        openInApp = openInApp,
                        actionType = InappActionType.fromAction(action),
                        scheme = scheme,
                        messageId = messageId
                    )
                )
            }
            return true
        }
    }

    init {

        webView = WebView(context.applicationContext).apply {
            settings.apply {
                javaScriptEnabled = true
                setLayerType(LAYER_TYPE_NONE, null)
                isHorizontalScrollBarEnabled=true
                isVerticalScrollBarEnabled=true
                javaScriptCanOpenWindowsAutomatically = true
                defaultTextEncodingName = Charsets.UTF_8.name()
                webChromeClient = mappChromeClient
                webViewClient = mappWebClient
                //useWideViewPort = true
                textZoom = 100
                domStorageEnabled = false
                cacheMode = LOAD_NO_CACHE
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        webView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        progressBar.layoutParams =
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
        addView(webView)
        addView(progressBar)
    }

    override fun onDetachedFromWindow() {
        webView.let {
            it.stopLoading()
            removeView(it)
        }
        removeView(progressBar)
        super.onDetachedFromWindow()
    }

    fun loadData(data: String) {
        webView.loadDataWithBaseURL(null, data, "text/html; charset=utf-8", "UTF-8", null)
    }

    fun setOnButtonClick(onButtonClick: ((ActionData) -> Unit)? = null) {
        this.onButtonClick = onButtonClick
    }
}