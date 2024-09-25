package com.appoxee.internal.ui.inapp.nativ

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
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
import com.appoxee.internal.util.LibExt.toColor
import kotlinx.coroutines.CoroutineScope

internal class FullscreenImageNativeTemplate<T : Message>(
    private val activity: Activity,
    inappActionHandler: InappActionHandler,
    private val message: T,
    scope: CoroutineScope,
    dispatchers: Dispatchers,
    private val onMessageClosed: ((T, TrackingKey, TrackingParams) -> Unit)? = null
) : Template(inappActionHandler, scope, dispatchers) {

    private lateinit var alertDialog: AlertDialog

    init {
        createTemplate()
    }

    private fun createTemplate() {
        val layoutRes = R.layout.me_inapp_background_image_fullscreen
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(layoutRes, null)

        alertDialog = AlertDialog.Builder(
            activity,
            android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen
        ).create().apply {
            onViewCreated(message, view) { onDismiss() }
            setupViews(activity, view, (message as NativeInappMessage))
            setView(view)
            setCancelable(false)
            window?.setWindowAnimations(R.style.CustomLeftDialogAnimation)
            setOnDismissListener {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                onMessageClosed?.invoke(message, trackingKeyResult, trackingParams)
            }
            window?.setBackgroundDrawableResource(R.drawable.me_round_rect_layout)
            view.setBackgroundColor(message.templateBackgroundColor.toColor())
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setupViews(
        context: Activity,
        view: View,
        message: NativeInappMessage,
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

    private fun onDismiss() {
        alertDialog.dismiss()
        job?.cancel()
    }

    override fun show() {
        alertDialog.show()
    }
}