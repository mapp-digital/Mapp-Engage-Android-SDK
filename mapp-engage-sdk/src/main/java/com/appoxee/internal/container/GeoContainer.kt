package com.appoxee.internal.container

import android.content.Context
import androidx.work.WorkManager
import com.appoxee.internal.geo.GeofenceClient
import com.appoxee.internal.geo.GeofenceClientImpl
import com.appoxee.internal.geo.GeofenceRegistry
import com.appoxee.internal.geo.GeofenceRegistryImpl
import com.appoxee.internal.geo.GeofenceScheduler
import com.appoxee.internal.geo.GeofenceSchedulerImpl
import com.appoxee.internal.geo.LocationProvider
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.provider.SystemInfoProvider
import com.appoxee.internal.util.DispatchersProvider

internal class GeoContainer(
    context: Context,
    private val systemInfoProvider: SystemInfoProvider,
    internal val engageApi: EngageApi,
    internal val dispatchersProvider: DispatchersProvider
) {
    private val workManager: WorkManager by lazy {
        WorkManager.getInstance(context.applicationContext)
    }

    private val locationProvider: LocationProvider by lazy {
        LocationProvider(context)
    }

    internal val geofenceClient: GeofenceClient by lazy {
        GeofenceClientImpl(context, locationProvider, engageApi)
    }

    internal val geofenceScheduler: GeofenceScheduler by lazy {
        GeofenceSchedulerImpl(dispatchersProvider, workManager)
    }

    internal val geofenceRegistry: GeofenceRegistry by lazy {
        GeofenceRegistryImpl(context, geofenceClient, systemInfoProvider, geofenceScheduler)
    }
}