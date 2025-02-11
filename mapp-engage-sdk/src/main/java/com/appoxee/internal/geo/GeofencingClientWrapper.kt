package com.appoxee.internal.geo

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import com.appoxee.internal.model.response.geo.Region
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

internal class GeofencingClientWrapper(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    // PendingIntent for geofence transitions
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            102,
            intent,
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun buildGeofenceList(regions: List<Region>, enterDelaySeconds: Int = 0): List<Geofence> {
        return regions.map { region ->
            val geofence = Geofence.Builder()
                .setRequestId(region.id.toString())
                .setCircularRegion(region.lat, region.lng, region.radius.toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)

            if (enterDelaySeconds > 0) {
                geofence.setLoiteringDelay(enterDelaySeconds * 1000)
                geofence.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT)
            } else {
                geofence.setLoiteringDelay(0)
                geofence.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            }

            geofence.build()
        }
    }

    // Add geofences to GeofencingClient
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    suspend fun addGeofences(
        geofences: List<Geofence>
    ) {
        removeGeofences()
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).await()
    }

    // Remove all geofences
    suspend fun removeGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent).await()
    }
}
