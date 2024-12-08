package com.appoxee.internal.geo

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.container.GeoContainer
import com.appoxee.internal.util.Logger
import kotlinx.coroutines.coroutineScope
import java.io.IOException

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
                ?: if (this@LocationUpdateWorker.runAttemptCount < 3) {
                    return@coroutineScope Result.retry()
                } else {
                    return@coroutineScope Result.failure()
                }

            val response = geoContainer.engageApi.getRegions(
                lat = location.latitude,
                lng = location.longitude,
                version = 0,
                pageSize = 50,
            )

            if (response.isSuccess()) {
                val regions = response.data?.payload?.regions?.let {
                    if (it.size > 100) it.sortedByDescending { it.id }.subList(0, 100) else it
                } ?: emptyList()
                Logger.d(TAG, "Regions: $regions")
                if (regions.isNotEmpty()) {
                    geoContainer.geofencingClientWrapper.apply {
                        val geofence = buildGeofenceList(regions)
                        addGeofences(geofence, onSuccess = {
                            Logger.d(TAG, "Geofences added: $geofence")
                        }, onFailure = {
                            Logger.e(TAG, "Error adding geofence: $geofence")
                        })
                    }
                }
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: IOException) {
            // Handle specific error, e.g., network issues
            Logger.e(TAG, e)
            Result.retry()
        } catch (e: Exception) {
            // Log or handle unexpected errors
            Logger.e(TAG, e)
            Result.failure()
        }
    }
}