package com.appoxee.internal.provider

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

interface IconProvider {
    fun getLargeIcon(): Bitmap?

    @DrawableRes
    fun getSmallIcon(): Int

    @DrawableRes
    fun getCustomSmallIcon(): Int

    fun getCustomLargeIcon(): Bitmap?

    @ColorInt
    fun getCustomIconColor(): Int
}