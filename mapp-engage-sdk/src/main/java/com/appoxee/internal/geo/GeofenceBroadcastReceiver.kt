package com.appoxee.internal.geo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.container.GeoContainer
import com.appoxee.internal.model.request.geo.GeoEvent
import com.appoxee.internal.util.Logger
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    private lateinit var appoxeeContainer: AppoxeeContainer
    private lateinit var scope: CoroutineScope

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun getAppoxeeContainer(context: Context): AppoxeeContainer {
        return AppoxeeContainer.getInstance(context.applicationContext)
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val geofencingEvent = intent?.let { GeofencingEvent.fromIntent(it) }
        if (geofencingEvent?.hasError() == true) {
            Logger.e("GeofenceReceiver", "Error: ${geofencingEvent.errorCode}")
            return
        }

        scope = CoroutineScope(SupervisorJob())

        appoxeeContainer = getAppoxeeContainer(context)

        val geoContainer = appoxeeContainer.geoContainer

        val geofenceTransition = geofencingEvent?.geofenceTransition
        val triggeringGeofences = geofencingEvent?.triggeringGeofences

        triggeringGeofences?.forEach { geofence ->
            val requestId = geofence.requestId
            val data = Data.Builder()
                .putDouble(GeoData.LATITUDE_KEY, geofence.latitude)
                .putDouble(GeoData.LONGITUDE_KEY, geofence.longitude)
                .putString(GeoData.REGION_ID_KEY, geofence.requestId)
                .putInt(GeoData.VERSION_KEY, 0)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            when (geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    Logger.i("GeofenceReceiver", "Entered geofence with ID: $requestId")
                    data.putInt(GeoData.EVENT_KEY, GeoEvent.ENTER.ordinal)
                    postEvent(geoContainer, data, constraints)
                }

                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    Logger.i("GeofenceReceiver", "Exited geofence with ID: $requestId")
                    data.putInt(GeoData.EVENT_KEY, GeoEvent.EXIT.ordinal)
                    postEvent(geoContainer, data, constraints)
                }

                Geofence.GEOFENCE_TRANSITION_DWELL -> {
                    Logger.i("GeofenceReceiver", "Dwell geofence with ID: $requestId")
                    data.putInt(GeoData.EVENT_KEY, GeoEvent.DWELL.ordinal)
                    postEvent(geoContainer, data, constraints)
                }

                else -> {
                    Logger.e("GeofenceReceiver", "Unknown transition type")
                }
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun postEvent(
        geoContainer: GeoContainer,
        data: Data.Builder,
        constraints: Constraints
    ) {
        scope.launch {
            geoContainer.geofenceScheduler.postGeofenceEvent(
                data = data.build(),
                constraints = constraints,
            )
        }
    }
}