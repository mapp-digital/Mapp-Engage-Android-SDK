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

    fun buildGeofenceList(regions: List<Region>): List<Geofence> {
        return regions.map { region ->
            Geofence.Builder()
                .setRequestId(region.id.toString())
                .setCircularRegion(region.lat, region.lng, region.radius.toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        }
    }

    // Add geofences to GeofencingClient
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun addGeofences(
        geofences: List<Geofence>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        removeGeofences({}, {})
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    // Remove all geofences
    fun removeGeofences(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }
}
