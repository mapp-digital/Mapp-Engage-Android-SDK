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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

internal class AppoxeeContainer(
    context: Context,
    storage: Storage
) {
    internal val deviceProvider: DeviceProvider by lazy { DeviceProviderImpl(context = context) }

    internal val baseScope: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

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

    internal val appoxeeAdapter: AppoxeeAdapter by lazy {
        AppoxeeAdapter(
            engageApi = engageApi,
            storage = storage
        )
    }

}