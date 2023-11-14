@file:Suppress("MemberVisibilityCanBePrivate")

package com.appoxee.internal.container

import android.app.Application
import com.appoxee.internal.AppoxeeAdapter
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.network.EngageApiImpl
import com.appoxee.internal.network.NetworkClient
import com.appoxee.internal.network.NetworkClientImpl
import com.appoxee.internal.provider.DeviceProvider
import com.appoxee.internal.provider.DeviceProviderImpl
import com.appoxee.internal.storage.PrefsStorageImpl
import com.appoxee.internal.storage.Storage
import com.appoxee.shared.AppoxeeOptions

internal class AppoxeeContainer(
    private val application: Application,
    private val storage: Storage
) {

    internal lateinit var options: AppoxeeOptions

    internal val deviceProvider: DeviceProvider by lazy { DeviceProviderImpl(context = application) }

    internal val networkClient: NetworkClient by lazy { NetworkClientImpl(options) }

    internal val engageApi: EngageApi by lazy {
        EngageApiImpl(
            networkClient = networkClient,
            storage = storage,
            deviceProvider = deviceProvider,
            options = options,
        )
    }

    internal val appoxeeAdapter: AppoxeeAdapter by lazy {
        AppoxeeAdapter(
            engageApi = engageApi,
            storage = storage
        )
    }

}