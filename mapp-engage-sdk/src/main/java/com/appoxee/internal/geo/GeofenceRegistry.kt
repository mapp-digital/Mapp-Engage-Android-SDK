package com.appoxee.internal.geo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.Data
import com.appoxee.internal.provider.SystemInfoProvider
import com.appoxee.internal.util.Logger
import com.appoxee.shared.GeoStatus
import com.appoxee.shared.GeofenceException

internal interface GeofenceRegistry {
    suspend fun startGeofencing(enterDelaySeconds: Int): GeoStatus
    suspend fun stopGeofencing(): GeoStatus
    fun hasRequiredPermissions(): Boolean
}

internal class GeofenceRegistryImpl(
    private val context: Context,
    private val geofenceClient: GeofenceClient,
    private val systemInfoProvider: SystemInfoProvider,
    private val geofenceScheduler: GeofenceScheduler
) : GeofenceRegistry {
    private val pendingIntent = geofenceClient.createGeofencePendingIntent()

    override suspend fun startGeofencing(enterDelaySeconds: Int): GeoStatus {
        if (!hasRequiredPermissions())
            return GeoStatus.GeoLocationPermissionsNotGranted()

        return try {
            val location = geofenceClient.getLocation()
            val regions = geofenceClient.getRegions(location)
            val geofences = geofenceClient.buildGeofenceList(regions, enterDelaySeconds)
            geofenceClient.addGeofences(geofences, pendingIntent)
            val data = Data.Builder().putInt("enterDelaySeconds", enterDelaySeconds).build()
            geofenceScheduler.scheduleRefreshGeofencesPeriodicWorker(data = data)
            GeoStatus.GeoStartedOk()
        } catch (e: GeofenceException) {
            e.geoStatus
        } catch (e: Exception) {
            GeoStatus(e.message ?: "Unknown error")
        }
    }

    override suspend fun stopGeofencing(): GeoStatus {
        return try {
            geofenceClient.removeGeofences(pendingIntent)
            geofenceScheduler.cancel()
            GeoStatus.GeoStoppedOk()
        } catch (e: GeofenceException) {
            e.geoStatus
        } catch (e: Exception) {
            Logger.e(this::class.java.name, e)
            GeoStatus(e.message ?: "Unknown error")
        }
    }

    @SuppressLint("InlinedApi")
    override fun hasRequiredPermissions(): Boolean {
        if (systemInfoProvider.currentSdkInt() <= Build.VERSION_CODES.M) {
            return true
        } else {
            val requiredPermissions = mutableListOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (systemInfoProvider.currentSdkInt() >= Build.VERSION_CODES.Q) {
                requiredPermissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }

            return requiredPermissions.all {
                ContextCompat.checkSelfPermission(
                    context,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
}