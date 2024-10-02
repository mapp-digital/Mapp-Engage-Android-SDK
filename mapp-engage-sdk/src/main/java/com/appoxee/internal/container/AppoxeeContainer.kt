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
import com.appoxee.internal.storage.Storage
import com.appoxee.internal.ui.ActivityLifecycleHandler
import com.appoxee.internal.util.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex

internal class AppoxeeContainer private constructor(
    context: Context,
    storage: Storage,
    dispatchers: Dispatchers,
) {
    companion object {
        private lateinit var instance: AppoxeeContainer
        private val mutex = Mutex()
        fun getInstance(
            context: Context,
            storage: Storage,
            dispatchers: Dispatchers
        ): AppoxeeContainer {
            if (!::instance.isInitialized) {
                synchronized(mutex) {
                    if (!::instance.isInitialized) {
                        instance = AppoxeeContainer(context, storage, dispatchers)
                    }
                }
            }
            return instance
        }
    }

    internal var localPushBroadcast: Class<*>? = null

    internal val deviceProvider: DeviceProvider by lazy { DeviceProviderImpl(context = context) }

    internal val baseScope: CoroutineScope by lazy { CoroutineScope(dispatchers.ioDispatcher + SupervisorJob()) }

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

    internal val activityLifecycleHandler: ActivityLifecycleHandler by lazy {
        ActivityLifecycleHandler(context.applicationContext)
    }

    internal val appoxeeAdapter: AppoxeeAdapter by lazy {
        AppoxeeAdapter(
            engageApi = engageApi,
            storage = storage,
            dispatchers = dispatchers
        )
    }

}