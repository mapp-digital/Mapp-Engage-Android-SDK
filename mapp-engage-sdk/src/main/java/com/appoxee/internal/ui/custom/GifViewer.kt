package com.appoxee.internal.ui.custom

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.size.Scale

class GifViewer(context: Context, attributeSet: AttributeSet?, defStyle: Int) :
    AppCompatImageView(context, attributeSet, defStyle) {
    constructor(context: Context) : this(context, null, 0)

    init {
        layoutParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
    }

    private val imageLoader = ImageLoader.Builder(context).components {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            add(ImageDecoderDecoder.Factory())
        } else {
            add(GifDecoder.Factory())
        }
    }.build()

    fun loadGif(gif: String?) {
        ImageRequest.Builder(context).data(gif)
            .target(this)
            .scale(Scale.FIT)
            .build().let {
                imageLoader.enqueue(it)
            }
    }
}