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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Scale
import com.appoxee.R
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.NativeInappMessage
import com.appoxee.internal.model.response.inapp.TrackingParams
import com.appoxee.internal.ui.inapp.InappActionHandler
import com.appoxee.internal.ui.inapp.Template
import com.appoxee.internal.util.Dispatchers
import com.appoxee.internal.util.LibExt.getDisplayMetrics
import com.appoxee.internal.util.LibExt.toColor
import com.appoxee.internal.util.LibExt.toPx
import kotlinx.coroutines.CoroutineScope

internal class StandardNativeTemplate<T : Message>(
    private val activity: Activity,
    inappActionHandler: InappActionHandler,
    private val message: T,
    scope: CoroutineScope,
    dispatchers: Dispatchers,
    private val onMessageClosed: ((T, TrackingKey, TrackingParams) -> Unit)? = null
) : Template(inappActionHandler, scope, dispatchers) {

    private lateinit var alertDialog: AlertDialog
    private var height: Int = 0
    private var width: Int = 0

    init {
        createTemplate()
    }

    private fun createTemplate() {
        val layoutRes = R.layout.me_inapp_standard
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(layoutRes, null)

        width =
            (activity.getDisplayMetrics().widthPixels * ((message.location?.width ?: 100) / 100f))
                .toInt()
        height =
            (activity.getDisplayMetrics().heightPixels * ((message.location?.height ?: 100) / 100f))
                .toInt()
        alertDialog = AlertDialog.Builder(activity).create().apply {
            onViewCreated(message, view) { onDismiss() }
            setupViews(activity, view, (message as NativeInappMessage))
            view.findViewById<ImageView>(R.id.ivImage)?.also {
                it.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (height * 0.5f).toInt()
                )
            }
            setView(view)
            setCancelable(false)
            setOnDismissListener {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                onMessageClosed?.invoke(message, trackingKeyResult, trackingParams)
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setupViews(
        activity: Activity,
        view: View,
        message: NativeInappMessage,
    ) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
            ImageRequest.Builder(activity)
                .data(message.imageUrl)
                .target(it)
                .scale(Scale.FILL)
                .build()
                .also {
                    ImageLoader(activity).enqueue(it)
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
            it.decorView.backgroundTintList =
                ColorStateList.valueOf((message as NativeInappMessage).templateBackgroundColor.toColor())
        }
    }
}