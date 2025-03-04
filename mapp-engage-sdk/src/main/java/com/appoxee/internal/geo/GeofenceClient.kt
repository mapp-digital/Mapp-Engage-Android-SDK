package com.appoxee.internal.geo

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.annotation.RequiresPermission
import com.appoxee.internal.model.response.geo.Region
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.util.CompatExt
import com.appoxee.shared.GeoStatus
import com.appoxee.shared.GeofenceException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

interface GeofenceClient {
    suspend fun getRegions(location: Location): List<Region>
    suspend fun getLocation(): Location
    suspend fun addGeofences(geofences: List<Geofence>, pendingIntent: PendingIntent)
    fun buildGeofenceList(regions: List<Region>, enterDelaySeconds: Int = 0): List<Geofence>
    fun removeGeofences(pendingIntent: PendingIntent)
    fun createGeofencePendingIntent(): PendingIntent
    fun getGeofencingRequestBuilder(): GeofencingRequest.Builder
    fun isGeofencingActive(): Boolean
}

internal class GeofenceClientImpl(
    private val context: Context,
    private val locationProvider: LocationProvider,
    private val engageApi: EngageApi,
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
) : GeofenceClient {

    override suspend fun getRegions(location: Location): List<Region> {
        val response = engageApi.getRegions(location.latitude, location.longitude, 0, 50)

        if (response.isSuccess()) {
            return response.data?.payload?.regions ?: emptyList()
        } else {
            throw GeofenceException(GeoStatus.GeoFailedGettingRegions())
        }
    }

    override suspend fun getLocation(): Location {
        return locationProvider.getCurrentLocation()
            ?: throw GeofenceException(GeoStatus.GeoLocationNotAvailable())
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override suspend fun addGeofences(geofences: List<Geofence>, pendingIntent: PendingIntent) {
        if (geofences.isEmpty()) throw GeofenceException(GeoStatus.GeoEmptyGeofencesList())

        val geofencingBuilder = getGeofencingRequestBuilder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)

        removeGeofences(pendingIntent)
        geofencingClient.addGeofences(geofencingBuilder.build(), pendingIntent)
    }

    override fun buildGeofenceList(regions: List<Region>, enterDelaySeconds: Int): List<Geofence> {
        return regions.map<Region, Geofence> { region ->
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


    override fun removeGeofences(pendingIntent: PendingIntent) {
        geofencingClient.removeGeofences(pendingIntent)
    }

    override fun createGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = CompatExt.PENDING_INTENT_MUTABLE_UPDATE_FLAGS
        return PendingIntent.getBroadcast(
            context,
            102,
            intent,
            flags
        )
    }

    override fun getGeofencingRequestBuilder(): GeofencingRequest.Builder {
        return GeofencingRequest.Builder()
    }

    override fun isGeofencingActive(): Boolean {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        val flags = CompatExt.PENDING_INTENT_NO_CREATE_FLAGS
        val pendingIntent =
            PendingIntent.getBroadcast(context, 102, intent, flags)

        return (pendingIntent != null)
    }

}