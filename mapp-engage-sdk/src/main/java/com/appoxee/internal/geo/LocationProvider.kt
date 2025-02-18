@file:Suppress("DEPRECATION")

package com.appoxee.internal.geo

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class LocationProvider(context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationRequest = createLocationRequest()

    private val locationCallback = LocationCallback()

    private val _locationUpdates = MutableStateFlow<Location?>(null)
    val locationUpdates: StateFlow<Location?> = _locationUpdates

    /**
     * Create instance of LocationRequest
     */
    private fun createLocationRequest(): LocationRequest {
        val interval = TimeUnit.MINUTES.toMillis(5)
        val fastestInterval = TimeUnit.MINUTES.toMillis(1)
        val longestInterval = TimeUnit.MINUTES.toMillis(10)
        val longestDistance = 50f
        val priority = Priority.PRIORITY_LOW_POWER

        val locationRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            LocationRequest.Builder(Priority.PRIORITY_LOW_POWER, interval)
                .setMaxUpdateDelayMillis(longestInterval)
                .setMinUpdateDistanceMeters(longestDistance)
                .setMinUpdateIntervalMillis(fastestInterval)
                .build()
        } else {
            LocationRequest.create().apply {
                this.interval = interval
                this.priority = priority
                this.maxWaitTime = longestInterval
                this.fastestInterval = fastestInterval
            }
        }
        return locationRequest
    }

    /**
     * Get the current location (latitude, longitude).
     * @return A [Location] object containing latitude and longitude.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = suspendCoroutine { continuation ->
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            continuation.resume(location)
        }.addOnFailureListener {
            continuation.resume(null)
        }
    }

    /**
     * Subscribe to location updates as a [Flow].
     * This allows Compose to observe continuous updates.
     */
    @SuppressLint("MissingPermission")
    fun subscribeToLocationUpdates(): Flow<Location> = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    trySend(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, callback, null)

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    /**
     * Start listening for location updates and post to LiveData.
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    /**
     * Stop listening for location updates.
     */
    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    open inner class LocationCallback : com.google.android.gms.location.LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.locations.forEach { location ->
                _locationUpdates.value = location
            }
        }
    }
}