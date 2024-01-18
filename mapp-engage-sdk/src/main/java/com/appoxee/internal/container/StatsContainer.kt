package com.appoxee.internal.container

import android.app.Application
import android.content.Context
import com.appoxee.internal.stats.StatsClient
import com.appoxee.internal.stats.StatsClientImpl

internal class StatsContainer(context: Context) {
    private val storageContainer = StorageContainer.getInstance(context)
    private val appoxeeContainer = AppoxeeContainer(context, storageContainer.storage)

    val statsClient: StatsClient by lazy {
        StatsClientImpl(appoxeeContainer.engageApi, appoxeeContainer.baseScope)
    }

}