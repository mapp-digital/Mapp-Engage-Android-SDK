package com.appoxee.internal.ui.banner

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Scale
import com.appoxee.R
import com.appoxee.internal.model.response.inapp.NativeInappMessage
import com.appoxee.internal.util.LibExt.toColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

internal class NativeBannerFactory : BannerFactory() {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    @SuppressLint("SourceLockedOrientationActivity")
    fun createBanner(
        context: Activity,
        message: NativeInappMessage,
        onMessageClosed: (NativeInappMessage) -> Unit
    ) {
        val delaySeconds = message.behaviour?.delaySeconds?.toLong() ?: 0
        scope.launch {
            delay(TimeUnit.SECONDS.toMillis(delaySeconds))
            withContext(Dispatchers.Main) {
                if (context.isDestroyed) return@withContext
                when (message.type) {
                    FULLSCREEN_TYPE_INAPP -> {
                        // alert dialog
                        createFullscreenInapp(context, message, onMessageClosed)
                    }

                    BANNER_TYPE_INAPP -> {
                        // window manager
                        createBannerInapp(context, message, onMessageClosed)
                    }

                    MODAL_TYPE_INAPP -> {
                        // alert dialog
                        createModalInapp(context, message, onMessageClosed)
                    }

                    else -> {
                        null
                    }
                }
            }
        }
    }

    private fun createFullscreenInapp(context: Activity, message: NativeInappMessage, onMessageClosed: (NativeInappMessage) -> Unit) {
        val layoutRes = getInappLayout(message)
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(layoutRes, null)

        AlertDialog.Builder(
            context,
            android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen
        ).create().apply {
            setupViews(context, view, message) {
                dismiss()
                scope.cancel()
            }
            setView(view)
            setCancelable(false)
            window?.setWindowAnimations(R.style.CustomLeftDialogAnimation)
            setOnDismissListener {
                context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                onMessageClosed(message)
            }
            show()
        }
    }

    private fun createBannerInapp(context: Activity, message: NativeInappMessage, onMessageClosed: (NativeInappMessage) -> Unit) {
        val layoutRes = getInappLayout(message)
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(layoutRes, null)

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val position = BannerPosition.fromValue(message.location?.position ?: 0)

        val layoutParams = LayoutParams(
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

        setupViews(context, view, message) {
            windowManager.removeViewImmediate(view)
            context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            scope.cancel()
            onMessageClosed(message)
        }
        windowManager.addView(view, layoutParams)
    }

    private fun createModalInapp(context: Activity, message: NativeInappMessage, onMessageClosed: (NativeInappMessage) -> Unit) {
        val layoutRes = getInappLayout(message)
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(layoutRes, null)

        AlertDialog.Builder(context, R.style.ModalDialogTheme).create().apply {
            setupViews(context, view, message) {
                dismiss()
                scope.cancel()
            }
            setView(view)
            setCancelable(false)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.let {
                val drawable = ContextCompat.getDrawable(context, R.drawable.me_round_rect_layout)
                drawable?.setTint(message.templateBackgroundColor.toColor())
                it.setBackgroundDrawable(drawable)
                it.setWindowAnimations(R.style.CustomLeftDialogAnimation)
            }
            setOnDismissListener {
                context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                onMessageClosed(message)
            }
            show()
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setupViews(
        context: Activity,
        view: View,
        message: NativeInappMessage,
        onDismiss: (() -> Unit)? = null
    ) {
        context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        view.setBackgroundColor(message.templateBackgroundColor.toColor())

        view.findViewById<TextView>(R.id.tvTitle)?.let {
            it.visibility = if (message.title.isEmpty()) View.GONE else View.VISIBLE
            it.text = message.title
            message.titleColor?.toColor()?.let { color -> it.setTextColor(color) }
        }

        view.findViewById<TextView>(R.id.tvContent)?.let {
            it.visibility = if (message.content.isEmpty()) View.GONE else View.VISIBLE
            it.text = message.content
            message.contentColor?.toColor()?.let { color -> it.setTextColor(color) }
        }

        view.findViewById<ImageView>(R.id.ivImage)?.let {
            it.visibility = if (message.imageUrl.isNullOrEmpty()) View.GONE else View.VISIBLE
            ImageRequest.Builder(context)
                .data(message.imageUrl)
                .target(it)
                .scale(Scale.FIT)
                .build()
                .also {
                    ImageLoader(context).enqueue(it)
                }
        }

        view.findViewById<Button>(R.id.btn1)?.let {
            message.buttons.getOrNull(0)?.let { btn ->
                it.visibility = if (btn.text.isEmpty()) View.GONE else View.VISIBLE
                it.text = btn.text
                it.backgroundTintList = ColorStateList.valueOf(btn.backgroundColor.toColor())
                it.setTextColor(btn.textColor.toColor())
                it.setOnClickListener {
                    Toast.makeText(context, btn.link, Toast.LENGTH_SHORT).show()
                    onDismiss?.invoke()
                }
            }
        }

        view.findViewById<Button>(R.id.btn2)?.let {
            message.buttons.getOrNull(1)?.let { btn ->
                it.visibility = if (btn.text.isEmpty()) View.GONE else View.VISIBLE
                it.text = btn.text
                it.backgroundTintList = ColorStateList.valueOf(btn.backgroundColor.toColor())
                it.setTextColor(btn.textColor.toColor())
                it.setOnClickListener {
                    Toast.makeText(context, btn.link, Toast.LENGTH_SHORT).show()
                    onDismiss?.invoke()
                }
            }
        }

        view.findViewById<ImageButton>(R.id.ibClose)?.let {
            it.setOnClickListener { onDismiss?.invoke() }
        }

        message.behaviour?.displaySeconds?.toLong()?.let { seconds ->
            scope.launch {
                delay(TimeUnit.SECONDS.toMillis(seconds))
                withContext(Dispatchers.Main) {
                    onDismiss?.invoke()
                }
            }
        }
    }

    @LayoutRes
    fun getInappLayout(message: NativeInappMessage): Int {
        return when (message.contentTemplateId) {
            ContentTemplates.BACKGROUND_IMAGE_STANDARD.template -> {
                R.layout.me_inapp_background_image_standard
            }

            ContentTemplates.FULLSCREEN.template -> {
                R.layout.me_inapp_fullscreen
            }

            ContentTemplates.BACKGROUND_IMAGE_FULLSCREEN.template -> {
                R.layout.me_inapp_background_image_fullscreen
            }

            ContentTemplates.BANNER_BOTTOM.template,
            ContentTemplates.BANNER_TOP.template -> {
                R.layout.me_inapp_banner_top_bottom
            }

            // ContentTemplates.STANDARD or any unknown template
            else -> {
                R.layout.me_inapp_standard
            }
        }
    }

}