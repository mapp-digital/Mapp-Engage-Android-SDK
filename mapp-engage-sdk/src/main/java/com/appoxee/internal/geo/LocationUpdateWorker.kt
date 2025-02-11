package com.appoxee.internal.geo

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.container.GeoContainer
import com.appoxee.internal.util.Logger
import com.appoxee.shared.GeoStatus
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

    private val appoxeeContainer: AppoxeeContainer by lazy {
        AppoxeeContainer.getInstance(context)
    }

    private val geoContainer: GeoContainer
        get() = appoxeeContainer.geoContainer

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override suspend fun doWork(): Result = coroutineScope {
        try {
            // Perform your task
            val location = geoContainer.locationProvider.getCurrentLocation()
                ?: throw GeofenceException(GeoStatus.GeoLocationNotAvailable())

            val response = geoContainer.engageApi.getRegions(
                lat = location.latitude,
                lng = location.longitude,
                version = 0,
                pageSize = 50,
            )

            val enterDelaySeconds = inputData.getInt("enterDelaySeconds", 0)

            if (response.isSuccess()) {
                val regions = response.data?.payload?.regions?.let {
                    if (it.size > 100) it.sortedByDescending { it.id }.subList(0, 100) else it
                } ?: emptyList()
                Logger.d(TAG, "Regions: $regions")
                if (regions.isNotEmpty()) {
                    val geofenceClient = geoContainer.geofencingClientWrapper
                    val geofence = geofenceClient.buildGeofenceList(regions, enterDelaySeconds)
                    geofenceClient.addGeofences(geofence)
                    Logger.d(TAG, "Geofences added: $geofence")
                }
                Result.success()
            } else {
                throw response.error ?: Throwable("Error getting regions data")
            }
        } catch (e: Exception) {
            // Log or handle unexpected errors
            Logger.e(TAG, "Exception in starting geofencing: $e")
            Result.failure()
        }
    }
}