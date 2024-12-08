package com.appoxee.internal.container

import android.content.Context
import androidx.work.WorkManager
import com.appoxee.internal.geo.GeoEventScheduler
import com.appoxee.internal.geo.GeofencingClientWrapper
import com.appoxee.internal.geo.LocationProvider
import com.appoxee.internal.geo.LocationUpdateScheduler
import com.appoxee.internal.geo.Scheduler
import com.appoxee.internal.network.EngageApi

internal class GeoContainer(context: Context, internal val engageApi: EngageApi) {

    private val storageContainer: StorageContainer by lazy {
        StorageContainer.getInstance(context)
    }

    private val workManager: WorkManager by lazy {
        WorkManager.getInstance(context.applicationContext)
    }

    internal val locationUpdateScheduler: Scheduler by lazy {
        LocationUpdateScheduler(workManager)
    }

    internal val geoEventScheduler: GeoEventScheduler by lazy {
        GeoEventScheduler(workManager)
    }

    internal val geofencingClientWrapper: GeofencingClientWrapper by lazy {
        GeofencingClientWrapper(context)
    }

    internal val locationProvider: LocationProvider by lazy {
        LocationProvider(context)
    }
}