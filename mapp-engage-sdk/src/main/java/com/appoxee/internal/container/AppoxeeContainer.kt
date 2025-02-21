@file:Suppress("MemberVisibilityCanBePrivate")

package com.appoxee.internal.container

import android.content.Context
import com.appoxee.internal.AppoxeeAdapter
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.network.EngageApiImpl
import com.appoxee.internal.network.NetworkClient
import com.appoxee.internal.network.NetworkClientImpl
import com.appoxee.internal.provider.DeviceProvider
import com.appoxee.internal.provider.DeviceProviderImpl
import com.appoxee.internal.provider.SystemInfoProvider
import com.appoxee.internal.provider.SystemInfoProviderImpl
import com.appoxee.internal.stats.StatsClient
import com.appoxee.internal.stats.StatsClientImpl
import com.appoxee.internal.storage.PrefsStorageImpl
import com.appoxee.internal.storage.Storage
import com.appoxee.internal.ui.ActivityLifecycleHandler
import com.appoxee.internal.util.Dispatchers
import com.appoxee.internal.util.DispatchersImpl
import com.appoxee.internal.util.Logger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.TimeUnit

internal class AppoxeeContainer private constructor(
    context: Context,
    dispatchers: Dispatchers,
) {
    companion object {
        private lateinit var instance: AppoxeeContainer
        private val mutex = Mutex()
        fun getInstance(
            context: Context,
            dispatchers: Dispatchers = DispatchersImpl()
        ): AppoxeeContainer {
            if (!::instance.isInitialized) {
                synchronized(mutex) {
                    if (!::instance.isInitialized) {
                        instance = AppoxeeContainer(context, dispatchers)
                    }
                }
            }
            return instance
        }
    }

    internal var localPushBroadcast: Class<*>? = null

    internal val defaultExceptionHandler: CoroutineExceptionHandler =
        CoroutineExceptionHandler { coroutineContext, throwable ->
            Logger.e(Thread.currentThread().name, throwable)
        }

    internal val storage: Storage by lazy {
        PrefsStorageImpl(
            context,
            TimeUnit.DAYS.toMillis(1),
            dispatchers
        )
    }

    internal val deviceProvider: DeviceProvider by lazy { DeviceProviderImpl(context = context) }

    internal val baseScope: CoroutineScope by lazy { CoroutineScope(dispatchers.defaultDispatcher + defaultExceptionHandler + SupervisorJob()) }

    internal val networkClient: NetworkClient by lazy {
        NetworkClientImpl(storage)
    }

    internal val engageApi: EngageApi by lazy {
        EngageApiImpl(
            networkClient = networkClient,
            storage = storage,
            deviceProvider = deviceProvider
        )
    }

    internal val statsClient: StatsClient by lazy {
        StatsClientImpl(engageApi, dispatchers)
    }

    internal val systemInfoProvider: SystemInfoProvider by lazy { SystemInfoProviderImpl() }

    internal val activityLifecycleHandler: ActivityLifecycleHandler by lazy {
        ActivityLifecycleHandler(
            context,
            statsClient,
            baseScope,
            dispatchers
        )
    }

    internal val geoContainer: GeoContainer by lazy {
        GeoContainer(context, systemInfoProvider, engageApi, dispatchers)
    }

    internal val appoxeeAdapter: AppoxeeAdapter by lazy {
        AppoxeeAdapter(
            engageApi = engageApi,
            storage = storage,
            dispatchers = dispatchers
        )
    }
}