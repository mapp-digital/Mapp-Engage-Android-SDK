package com.appoxee.internal.geo

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.util.Logger
import kotlinx.coroutines.coroutineScope

/**
 * Worker to periodically monitor location updates and send them to the backend server.
 * Based on current location, server returns list of the nearest geofencing locations.
 * List of geofences are then injected to the geofencing client, after which geofencing client monitors
 * if device entered/exit some of the geofences, and triggers proper event when it does.
 */
internal class LocationUpdateWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    companion object {
        const val WORK_NAME = "LocationUpdateWorker"
    }

    private val TAG = LocationUpdateWorker::class.java.name

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override suspend fun doWork(): Result = coroutineScope {
        try {
            val appoxeeContainer = AppoxeeContainer.getInstance(applicationContext)
            val geofenceRegistry = appoxeeContainer.geoContainer.geofenceRegistry
            val enterDelaySeconds = inputData.getInt("enterDelaySeconds", 0)
            geofenceRegistry.startGeofencing(enterDelaySeconds)
        } catch (e: Exception) {
            // Log or handle unexpected errors
            Logger.e(TAG, "Exception in starting geofencing: $e")
        }
        Result.success()
    }
}