package com.appoxee.internal.ui.push.style

import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.appoxee.internal.ui.push.model.PushData

internal class NotificationImageStyle(
    private val pushData: PushData,
    private val imageLoader: NotificationImageLoader = NotificationImageLoader.default
) : NotificationStyle {
    override suspend fun getStyle(): NotificationCompat.Style {
        val bitmap = imageLoader.load(pushData.iosApxMedia)
            ?: return NotificationTextStyle(pushData).getStyle()
        return NotificationCompat.BigPictureStyle()
            .setBigContentTitle(pushData.title)
            .bigPicture(bitmap)
            .bigLargeIcon(null as Bitmap?)
    }
}
