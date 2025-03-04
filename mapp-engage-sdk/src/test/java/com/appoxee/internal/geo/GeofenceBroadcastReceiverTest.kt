package com.appoxee.internal.geo

import android.content.Context
import android.content.Intent
import android.util.Log
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.container.GeoContainer
import com.appoxee.internal.model.request.geo.GeoEvent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.common.truth.Truth
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class GeofenceBroadcastReceiverTest {
    private lateinit var receiver: GeofenceBroadcastReceiver
    private val context = mockk<Context>(relaxed = true)
    private val intent = mockk<Intent>(relaxed = true)
    private val geofencingEvent = mockk<GeofencingEvent>(relaxed = true)
    private val geoContainer = mockk<GeoContainer>(relaxed = true)
    private val geoEventScheduler = mockk<GeofenceSchedulerImpl>(relaxed = true)
    private val appoxeeContainer = mockk<AppoxeeContainer>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } answers { 0 }
        every { Log.i(any(), any(), any()) } answers { 0 }

        every { context.applicationContext } answers { mockk(relaxed = true) }

        receiver = spyk(GeofenceBroadcastReceiver())

        every { receiver.getAppoxeeContainer(any()) } answers { appoxeeContainer }
        // Mock static methods
        mockkStatic(GeofencingEvent::class)
        every { GeofencingEvent.fromIntent(intent) } returns geofencingEvent

        every { geoContainer.geofenceScheduler } answers { geoEventScheduler }
        every { appoxeeContainer.geoContainer } answers { geoContainer }
    }

    @After
    fun tearDown() {
    }

    @Test
    fun `onReceive handles geofence transition ENTER correctly`() = runTest {
        // Mock data
        val geofence = mockk<Geofence>().apply {
            every { requestId } returns "geo1"
            every { latitude } returns 10.0
            every { longitude } returns 20.0
            every { transitionTypes } returns Geofence.GEOFENCE_TRANSITION_ENTER
        }

        every { geofencingEvent.hasError() } returns false
        every { geofencingEvent.geofenceTransition } returns Geofence.GEOFENCE_TRANSITION_ENTER
        every { geofencingEvent.triggeringGeofences } returns listOf(geofence)

        // Trigger the receiver
        receiver.onReceive(context, intent)

        // Verify that the scheduler was invoked
        coVerify {
            geoEventScheduler.postGeofenceEvent(
                data = coWithArg {
                    Truth.assertThat(it.keyValueMap["latitude"]).isEqualTo(10.0)
                    Truth.assertThat(it.keyValueMap["longitude"]).isEqualTo(20.0)
                    Truth.assertThat(it.keyValueMap["regionId"]).isEqualTo("geo1")
                    Truth.assertThat(it.keyValueMap["geoEvent"]).isEqualTo(GeoEvent.ENTER.ordinal)
                },
                constraints = any(),
            )
        }
    }

    @Test
    fun `onReceive doesn't trigger event when geofencing event has error`() {
        mockkStatic(GeofencingEvent::class)
        mockkStatic(Log::class)

        val mockGeofencingEvent = mockk<GeofencingEvent>(relaxed = true)

        every { mockGeofencingEvent.hasError() } returns true
        every { GeofencingEvent.fromIntent(intent) } returns mockGeofencingEvent
        every { Log.e(any(), any(), any()) } returns 0

        receiver.onReceive(context, intent)

        verifyOrder {
            Log.e(any(), any(), any())
        }

        coVerify(exactly = 0) {
            receiver.postEvent(any(), any(), any())
        }
    }

    @Test
    fun `onReceive doesn't trigger event when geofencing event is not enter exit or dwell`() {
        mockkStatic(GeofencingEvent::class)
        mockkStatic(Log::class)

        val geofence = mockk<Geofence>().apply {
            every { requestId } returns "geo1"
            every { latitude } returns 10.0
            every { longitude } returns 20.0
            every { transitionTypes } returns Geofence.GEOFENCE_TRANSITION_ENTER
        }

        every { geofencingEvent.hasError() } returns false
        every { geofencingEvent.geofenceTransition } returns 0
        every { geofencingEvent.triggeringGeofences } returns listOf(geofence)
        every { GeofencingEvent.fromIntent(intent) } returns geofencingEvent
        every { Log.e(any(), any(), any()) } returns 0

        receiver.onReceive(context, intent)

        verifyOrder {
            Log.e(any(), any(), any())
        }

        coVerify(exactly = 0) {
            receiver.postEvent(any(), any(), any())
        }
    }
}