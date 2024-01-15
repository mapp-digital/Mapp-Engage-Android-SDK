package com.appoxee.internal.ui.custom

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import android.widget.ImageButton
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import androidx.media3.common.util.UnstableApi
import com.appoxee.R
import com.appoxee.internal.push.model.NotificationType
import com.appoxee.internal.push.model.PushData
import com.appoxee.internal.util.CompatExt.getParcelableCompat
import com.appoxee.internal.util.LibExt.toPx

internal class MediaDialog : DialogFragment() {
    companion object {
        fun getInstance(pushData: PushData): MediaDialog {
            val bundle = Bundle().apply {
                putParcelable("pushData", pushData)
            }
            val mediaDialog = MediaDialog()
            mediaDialog.arguments = bundle
            return mediaDialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NO_TITLE, R.style.Mapp_MyActivityDialog)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FrameLayout(requireContext()).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view as FrameLayout?)?.let { container ->
            addMediaView(container)
            addCloseButton(container)
        }
    }

    private fun addMediaView(container: ViewGroup?) {
        val pushData = arguments?.getParcelableCompat<PushData>("pushData") ?: return
        when (NotificationType.fromString(pushData.type)) {
            NotificationType.GIF -> {
                addGifView(container, pushData)
            }

            NotificationType.VIDEO -> {
                addVideoView(container, pushData)
            }

            else -> {

            }
        }
    }

    private fun addGifView(container: ViewGroup?, pushData: PushData) {
        val gifViewer = GifViewer(requireContext())
        container?.addView(gifViewer)
        gifViewer.loadGif(pushData.iosApxMedia)
    }

    @OptIn(UnstableApi::class)
    private fun addVideoView(container: ViewGroup?, pushData: PushData) {
        val uri = Uri.parse(pushData.iosApxMedia)
        val playerView = VideoPlayer(requireContext(), uri)
        container?.addView(playerView)
    }

    private fun addCloseButton(container: ViewGroup?) {
        ImageButton(requireContext()).apply {
            val ctx = requireContext()
            val size = ctx.toPx(50)
            val padding = ctx.toPx(10)
            layoutParams = LayoutParams(size, size)
                .apply {
                    gravity = Gravity.TOP or Gravity.END
                    setPadding(padding)
                }
        }.apply {
            setOnClickListener {
                dismissAllowingStateLoss()
            }
            scaleType = ImageView.ScaleType.CENTER
            setBackgroundColor(Color.TRANSPARENT)
            setImageResource(R.drawable.mapp_ic_close)
            imageTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.white)
            container?.addView(this)
        }
    }
}