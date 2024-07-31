package com.appoxee.internal.provider

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmapOrNull

class IconProviderImpl(private val context: Context) : IconProvider {
    override fun getLargeIcon(): Bitmap? {
        val iconId = context.packageManager.getApplicationInfo(
            context.packageName, PackageManager.GET_META_DATA
        ).icon
        val drawable = ContextCompat.getDrawable(context, iconId)
        return drawable?.toBitmapOrNull()
    }

    override fun getSmallIcon(): Int {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        return appInfo.icon
    }

    @TargetApi(value = Build.VERSION_CODES.M)
    override fun getSmallIconApi23(): IconCompat? {
        val bitmap = context.packageManager.defaultActivityIcon.toBitmapOrNull()
        return bitmap?.let { IconCompat.createWithBitmap(bitmap) }
    }
}