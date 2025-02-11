package com.appoxee.internal.container

import android.content.Context
import com.appoxee.internal.stats.StatsClient
import com.appoxee.internal.stats.StatsClientImpl
import com.appoxee.internal.util.Dispatchers
import com.appoxee.internal.util.DispatchersImpl

internal class StatsContainer(
    context: Context,
    dispatchers: Dispatchers = DispatchersImpl(),
) {
    val storageContainer: StorageContainer by lazy { StorageContainer.getInstance(context) }
    val appoxeeContainer: AppoxeeContainer = AppoxeeContainer.getInstance(
        context,
        storageContainer.storage,
        dispatchers,
    )

    val statsClient: StatsClient by lazy {
        StatsClientImpl(appoxeeContainer.engageApi, dispatchers)
    }
}