package com.appoxee.internal

import android.app.Application
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.network.EngageApiImpl
import com.appoxee.internal.network.NetworkClient
import com.appoxee.internal.network.NetworkClientImpl
import com.appoxee.internal.provider.DeviceProvider
import com.appoxee.internal.provider.DeviceProviderImpl
import com.appoxee.shared.AppoxeeOptions

internal class AppoxeeContainer(application: Application, options: AppoxeeOptions) {

    private val networkClient: NetworkClient by lazy { NetworkClientImpl(options) }

    private val deviceProvider: DeviceProvider by lazy { DeviceProviderImpl(context = application) }

    private val engageApi: EngageApi by lazy {
        EngageApiImpl(
            networkClient = networkClient,
            deviceProvider = deviceProvider,
            options = options
        )
    }

    val appoxeeAdapter: AppoxeeAdapter by lazy {
        AppoxeeAdapter(
            deviceProvider = deviceProvider,
            engageApi = engageApi
        )
    }

}