package com.appoxee.internal.ui.inapp.nativ

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Space
import android.widget.TextView
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Scale
import com.appoxee.R
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.NativeInappMessage
import com.appoxee.internal.ui.inapp.ActionHandler
import com.appoxee.internal.ui.inapp.Template
import com.appoxee.internal.util.LibExt.toColor
import com.appoxee.internal.util.LibExt.toPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class FullscreenNativeTemplate<T : Message>(
    private val activity: Activity,
    private val actionHandler: ActionHandler,
    private val message: T,
    private val scope: CoroutineScope,
    private val onMessageClosed: ((T) -> Unit)? = null
) : Template {

    private var job: Job? = null
    private lateinit var alertDialog: AlertDialog

    init {
        createTemplate()
    }

    private fun createTemplate() {
        val layoutRes = R.layout.me_inapp_fullscreen
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(layoutRes, null)

        alertDialog = AlertDialog.Builder(
            activity,
            android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen
        ).create().apply {
            setupViews(activity, view, (message as NativeInappMessage)) {
                dismiss()
                job?.cancel()
            }
            setView(view)
            setCancelable(false)
            window?.setWindowAnimations(R.style.CustomLeftDialogAnimation)
            setOnDismissListener {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                onMessageClosed?.invoke(message)
            }
            window?.setBackgroundDrawableResource(R.drawable.me_round_rect_layout)
            view.setBackgroundColor(message.templateBackgroundColor.toColor())
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
                it.visibility = if (btn.text.isEmpty()) View.GONE else View.VISIBLE
                it.text = btn.text
                it.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = activity.toPx(buttonRadius).toFloat()
                }
                it.backgroundTintList = ColorStateList.valueOf(btn.backgroundColor.toColor())
                it.setTextColor(btn.textColor.toColor())
                it.setOnClickListener {
                    actionHandler.handleAction(btn)
                    onDismiss?.invoke()
                }
            }
        }

        view.findViewById<Button>(R.id.btn2)?.let {
            message.buttons.getOrNull(1)?.let { btn ->
                it.visibility = if (btn.text.isEmpty()) View.GONE else View.VISIBLE
                it.text = btn.text
                it.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = activity.toPx(buttonRadius).toFloat()
                }
                it.backgroundTintList = ColorStateList.valueOf(btn.backgroundColor.toColor())
                it.setTextColor(btn.textColor.toColor())
                it.setOnClickListener {
                    actionHandler.handleAction(btn)
                    onDismiss?.invoke()
                }
            }
        }

        view.findViewById<Space>(R.id.btnSpacer)?.let {
            if (message.buttons.size == 2) it.visibility = View.VISIBLE else View.GONE
        }

        view.findViewById<ImageButton>(R.id.ibClose)?.let {
            it.setOnClickListener { onDismiss?.invoke() }
        }

        message.behaviour?.displaySeconds?.toLong()?.let { seconds ->
            job = scope.launch {
                delay(TimeUnit.SECONDS.toMillis(seconds))
                withContext(Dispatchers.Main) {
                    onDismiss?.invoke()
                }
            }
        }
    }


    override fun show() {

    }
}