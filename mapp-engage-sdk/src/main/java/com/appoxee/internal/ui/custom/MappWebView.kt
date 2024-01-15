package com.appoxee.internal.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.Gravity
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings.LOAD_NO_CACHE
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.appoxee.internal.util.Logger

@SuppressLint("SetJavaScriptEnabled")
class MappWebView private constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyle: Int,
    defStyleRes: Int
) :
    FrameLayout(context, attrs, defStyle, defStyleRes) {
    private constructor(context: Context) : this(context, null, 0, 0)

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private lateinit var instance: MappWebView

        @JvmStatic
        @Synchronized
        internal fun getInstance(context: Context): MappWebView {
            if (!::instance.isInitialized) {
                instance = MappWebView(context)
                instance.loadUrl("about:blank")
                instance.webView.let {
                    it.stopLoading()
                    it.clearCache(true)
                }
            }
            return instance
        }
    }

    private val webView: WebView

    private lateinit var progressBar: ProgressBar

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
    }

    init {
        progressBar = ProgressBar(context.applicationContext).apply {
            isIndeterminate = true
        }

        webView = WebView(context.applicationContext).apply {
            settings.apply {
                javaScriptEnabled = true
                setLayerType(LAYER_TYPE_HARDWARE, null)
                javaScriptCanOpenWindowsAutomatically = true
                defaultTextEncodingName = Charsets.UTF_8.name()
                webChromeClient = mappChromeClient
                webViewClient = mappWebClient
                useWideViewPort = true
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

    fun loadUrl(url: String) {
        webView.loadUrl(url)
    }
}