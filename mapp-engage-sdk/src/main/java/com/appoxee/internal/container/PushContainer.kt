package com.appoxee.internal.container

import android.content.Context
import com.appoxee.internal.push.PushManager
import com.appoxee.internal.push.PushManagerImpl
import com.appoxee.shared.AppoxeeOptions

internal class PushContainer(context: Context) {
    internal lateinit var options: AppoxeeOptions

    internal val pushManager: PushManager by lazy { PushManagerImpl(context, options) }
}