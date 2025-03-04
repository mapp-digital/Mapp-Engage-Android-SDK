@file:Suppress("MemberVisibilityCanBePrivate")

package com.appoxee.internal.container

import android.content.Context
import com.appoxee.internal.AppoxeeAdapter
import com.appoxee.internal.migration.MigrationHelper
import com.appoxee.internal.migration.MigrationHelperImpl
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
import com.appoxee.internal.util.DispatchersProvider
import com.appoxee.internal.util.DispatchersProviderImpl
import com.appoxee.internal.util.Logger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex

internal class AppoxeeContainer private constructor(
    context: Context,
    val dispatchersProvider: DispatchersProvider = DispatchersProviderImpl(),
) {
    companion object {
        private lateinit var instance: AppoxeeContainer
        private val mutex = Mutex()
        fun getInstance(
            context: Context,
            dispatchersProvider: DispatchersProvider = DispatchersProviderImpl()
        ): AppoxeeContainer {
            if (!::instance.isInitialized) {
                synchronized(mutex) {
                    if (!::instance.isInitialized) {
                        instance = AppoxeeContainer(context, dispatchersProvider)
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
            context = context,
            dispatchersProvider = dispatchersProvider
        )
    }

    internal val deviceProvider: DeviceProvider by lazy { DeviceProviderImpl(context = context) }

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
        StatsClientImpl(engageApi, dispatchersProvider)
    }

    internal val systemInfoProvider: SystemInfoProvider by lazy { SystemInfoProviderImpl() }

    internal val activityLifecycleHandler: ActivityLifecycleHandler by lazy {
        ActivityLifecycleHandler(
            context,
            statsClient,
            dispatchersProvider
        )
    }

    internal val geoContainer: GeoContainer by lazy {
        GeoContainer(context, systemInfoProvider, engageApi, dispatchersProvider)
    }

    internal val appoxeeAdapter: AppoxeeAdapter by lazy {
        AppoxeeAdapter(
            engageApi = engageApi,
            storage = storage,
        )
    }

    internal val migrationHelper: MigrationHelper by lazy { MigrationHelperImpl(context) }
}