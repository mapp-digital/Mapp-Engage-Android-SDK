@file:OptIn(UnstableApi::class)

package com.appoxee.internal.ui.custom

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.core.view.children
import androidx.core.view.setMargins
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import com.appoxee.internal.util.LibExt.toPx

/**
 * Video player is custom class to show video content from some Uri source
 * It is simplified ExoPlayer with some customization to provide minimalistic design
 */
class VideoPlayer(context: Context, attributeSet: AttributeSet?, defStyle: Int) :
    FrameLayout(context, attributeSet, defStyle) {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    constructor(context: Context, uri: Uri) : this(context, null, 0) {
        playUri(uri)
    }


    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    playerView.postDelayed({
                        playerView.visibility = VISIBLE
                        player.play()
                    }, 10)
                }

                Player.STATE_BUFFERING -> {
                    playerView.showController()
                }

                Player.STATE_ENDED -> {
                    playerView.showController()
                }

                Player.STATE_IDLE -> {
                }
            }
        }
    }

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
            setBackgroundColor(0)
        }
        createPlayer()
    }

    @OptIn(UnstableApi::class)
    private fun createPlayer() {
        // create playerView and setup it's look
        playerView = PlayerView(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            visibility = View.INVISIBLE
            layoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
        }.also {
            this.addView(it)
            it.controllerAutoShow = true
            it.controllerHideOnTouch = true
            it.controllerShowTimeoutMs = 1000
            it.setShowSubtitleButton(false)
            it.setShowVrButton(false)
            it.children.forEach { view ->
                if (view is PlayerControlView) {
                    (view as PlayerControlView?)?.let { controls ->
                        controls.setShowNextButton(false)
                        controls.setShowPreviousButton(false)
                        controls.setShowFastForwardButton(false)
                        controls.setShowRewindButton(false)
                        controls.setShowPlayButtonIfPlaybackIsSuppressed(false)

                        // ExoPlayerView doesn't have public method to show/hide bottom bar which holds video timings
                        // currently to disable this view, only way is to use some sort of hack
                        controls.children
                            .filter { child -> child is FrameLayout && child.id == androidx.media3.ui.R.id.exo_bottom_bar }
                            .firstOrNull()
                            ?.visibility = View.GONE

                        // ExoPlayerView doesn't have public method to access progressBar and change it's values
                        // We also use some querying to access progressBar and change it's parameters
                        controls.children
                            .filter { child -> child is DefaultTimeBar || child.id == androidx.media3.ui.R.id.exo_progress_placeholder }
                            .firstOrNull()?.let { child ->
                                (child.layoutParams as LayoutParams).apply {
                                    gravity = Gravity.BOTTOM
                                    setMargins(context.toPx(5))
                                }
                            }
                    }
                }
            }
        }

        // initialize ExoPlayer
        player = ExoPlayer.Builder(context)
            .setUseLazyPreparation(true)
            .build().also { player ->
                player.addListener(playerListener)
            }
    }

    private fun playUri(uri: Uri) {
        player.addMediaItem(MediaItem.fromUri(uri))
        player.createMessage { _, _ ->
            playerView.hideController()
        }.setLooper(Looper.getMainLooper()).setPosition(1000).send()
        playerView.player = player
        player.prepare()
    }

    override fun onDetachedFromWindow() {
        player.removeListener(playerListener)
        player.stop()
        player.release()
        removeAllViews()
        super.onDetachedFromWindow()
    }
}