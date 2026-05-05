package com.appoxee.internal.provider

import android.os.Build

internal class SystemInfoProviderImpl : SystemInfoProvider {
    override fun currentSdkInt(): Int = Build.VERSION.SDK_INT
}