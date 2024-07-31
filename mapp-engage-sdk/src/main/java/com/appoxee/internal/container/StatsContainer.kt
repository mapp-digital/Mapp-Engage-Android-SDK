package com.appoxee.internal.container

import android.content.Context
import com.appoxee.internal.stats.StatsClient
import com.appoxee.internal.stats.StatsClientImpl

internal class StatsContainer(
    context: Context
) {
    private val storageContainer: StorageContainer by lazy { StorageContainer.getInstance(context) }
    private val appoxeeContainer: AppoxeeContainer by lazy {
        AppoxeeContainer(
            context,
            storageContainer.storage
        )
    }
    val statsClient: StatsClient by lazy {
        StatsClientImpl(appoxeeContainer.engageApi, appoxeeContainer.baseScope)
    }

}