package com.appoxee.internal.push.style

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.core.app.NotificationCompat
import com.appoxee.internal.push.model.PushData


internal class NotificationVideoStyle(private val pushData: PushData) : NotificationStyle {
    override suspend fun getStyle(): NotificationCompat.Style {
        val bitmap = getBitmap(pushData.iosApxMedia)
        return NotificationCompat.BigPictureStyle()
            .setBigContentTitle(pushData.title)
            .bigPicture(bitmap)
            .bigLargeIcon(null as Bitmap?)
    }

    private fun getBitmap(path: String?): Bitmap? {
        if (path.isNullOrEmpty()) return null
        var bitmap: Bitmap? = null
        var mediaMetadataRetriever: MediaMetadataRetriever? = null
        try {
            mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(path, HashMap<String, String>())
            //   mediaMetadataRetriever.setDataSource(videoPath);
            bitmap = mediaMetadataRetriever.frameAtTime
        } catch (e: Exception) {
            e.printStackTrace()
            throw Throwable("Exception in retriveVideoFrameFromVideo(String videoPath)" + e.message)
        } finally {
            mediaMetadataRetriever?.release()
        }
        return bitmap
    }
}