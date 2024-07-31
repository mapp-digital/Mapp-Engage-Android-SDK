package com.appoxee.internal.provider

import android.graphics.Bitmap
import androidx.core.graphics.drawable.IconCompat

interface IconProvider {
    fun getLargeIcon(): Bitmap?
    fun getSmallIcon(): Int
    fun getSmallIconApi23(): IconCompat?
}