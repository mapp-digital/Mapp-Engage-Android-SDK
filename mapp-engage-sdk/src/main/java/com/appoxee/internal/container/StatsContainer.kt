package com.appoxee.internal.container

import android.content.Context
import com.appoxee.Appoxee
import com.appoxee.internal.AppoxeeImpl
import com.appoxee.internal.stats.StatsClient
import com.appoxee.internal.stats.StatsClientImpl

internal class StatsContainer(context: Context) {
    private val appoxee: AppoxeeImpl

    init {
        Appoxee.engage(context)
        appoxee = Appoxee.instance() as AppoxeeImpl
    }

    val statsClient: StatsClient by lazy {
        StatsClientImpl(appoxee.appoxeeAdapter)
    }

}