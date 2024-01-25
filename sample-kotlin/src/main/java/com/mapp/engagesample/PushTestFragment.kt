package com.mapp.engagesample

import android.app.PendingIntent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import eu.brrm.shared_ui.LocalNotifications.createNotification
import eu.brrm.shared_ui.databinding.FragmentPushTestBinding

class PushTestFragment : Fragment() {

    private var _binding: FragmentPushTestBinding? = null
    private val binding: FragmentPushTestBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPushTestBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBrowser.setOnClickListener {
            try {
                createBrowserPendingIntent()
            } catch (_: Exception) {
            }
        }

        binding.btnGif.setOnClickListener {
            try {
                createGifNotification()
            } catch (_: Exception) {
            }
        }

        binding.btnVideo.setOnClickListener {
            try {
                createVideoNotification()
            } catch (_: Exception) {
            }
        }
    }

    @Throws(PendingIntent.CanceledException::class)
    fun createGifNotification() {
        createNotification(
            requireContext(),
            mapOf("apx_dpl" to "https://developer.android.com/training/dependency-injection/manual"),
            mapOf(
                "gif" to "https://cook.shortest-route.com/l3tech/imgproxy/img/768531997/nyan-cat.gif"
            ),
            listOf(mapOf("apx_url" to "http://www.google.com"))
        )
    }

    @Throws(PendingIntent.CanceledException::class)
    fun createVideoNotification() {
        createNotification(
            requireContext(),
            mapOf("apx_dpl" to "https://developer.android.com/training/dependency-injection/manual"),
            mapOf(
                "video" to "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
            ),
            listOf(mapOf("apx_url" to "http://www.google.com"))
        )
    }

    @Throws(PendingIntent.CanceledException::class)
    fun createBrowserPendingIntent() {
        createNotification(
            requireContext(),
            mapOf("apx_url" to "https://developer.android.com/training/dependency-injection/manual"),
            null,
            listOf(mapOf("apx_dpl" to "http://www.google.com"))
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}