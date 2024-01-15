package com.appoxee.internal.push.style

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.appoxee.internal.push.model.PushData
import java.net.URL

internal class NotificationImageStyle(private val pushData: PushData) : NotificationStyle {
    override suspend fun getStyle(): NotificationCompat.Style {
        val bitmap = getBitmap(pushData.iosApxMedia)
        return NotificationCompat.BigPictureStyle()
            .setBigContentTitle(pushData.title)
            .bigPicture(bitmap)
            .bigLargeIcon(null as Bitmap?)
    }

    private fun getBitmap(path: String?): Bitmap? {
        if (path.isNullOrEmpty()) return null
        return try {
            val url = URL(path)
            BitmapFactory.decodeStream(url.openStream())
        } catch (e: Exception) {
            null
        }
    }
}