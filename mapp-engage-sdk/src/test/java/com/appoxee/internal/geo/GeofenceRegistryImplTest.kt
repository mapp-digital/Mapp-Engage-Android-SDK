package com.appoxee.internal.geo

import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.appoxee.internal.model.response.geo.Region
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.provider.SystemInfoProvider
import com.appoxee.shared.GeoStatus
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class GeofenceRegistryImplTest {
    private lateinit var mockContext: Context

    private lateinit var mockPackageManager: PackageManager

    private lateinit var mockGeofenceClient: GeofenceClient

    private lateinit var mockLocationProvider: LocationProvider

    private lateinit var mockEngageApi: EngageApi

    private lateinit var mockGeofencingClient: GeofencingClient

    private lateinit var mockPendingIntent: PendingIntent

    private lateinit var geofenceScheduler: GeofenceScheduler

    private lateinit var mockSystemInfoProvider: SystemInfoProvider

    private lateinit var geofenceRegistry: GeofenceRegistry

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockPackageManager = mockk(relaxed = true)
        mockPendingIntent = mockk(relaxed = true)
        mockLocationProvider = mockk(relaxed = true)
        geofenceScheduler = mockk(relaxed = true)
        mockEngageApi = mockk(relaxed = true)
        mockSystemInfoProvider = mockk(relaxed = true)
        mockGeofencingClient = mockk(relaxed = true)
        mockGeofenceClient =
            spyk(
                GeofenceClientImpl(
                    mockContext,
                    mockLocationProvider,
                    mockEngageApi,
                    mockGeofencingClient
                )
            )
        every { mockContext.packageManager } returns mockPackageManager
        every { mockGeofenceClient.createGeofencePendingIntent() } returns mockPendingIntent
        every { mockSystemInfoProvider.currentSdkInt() } returns Build.VERSION_CODES.TIRAMISU
        coEvery {
            geofenceScheduler.scheduleRefreshGeofencesPeriodicWorker(
                any(),
                any(),
                any()
            )
        } coAnswers { coJustRun { Unit } }

        coEvery { geofenceScheduler.cancel() } just runs
        coEvery { geofenceScheduler.postGeofenceEvent(any(), any()) } just runs
        coEvery {
            geofenceScheduler.scheduleRefreshGeofencesPeriodicWorker(
                any(),
                any(),
                any()
            )
        } just runs
        every { geofenceScheduler.cancel() } just runs

        geofenceRegistry =
            spyk(
                GeofenceRegistryImpl(
                    mockContext,
                    mockGeofenceClient,
                    mockSystemInfoProvider,
                    geofenceScheduler
                )
            )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `startGeofencing should return GeoStartedOk when permissions are granted`() = runTest {
        // Mock successful location retrieval and geofence setup
        val mockLocation = mockk<Location>()
        val mockRegions = listOf(mockk<Region>())
        val mockGeofences = listOf(mockk<Geofence>())

        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(
                any(),
                any()
            )
        } returns PackageManager.PERMISSION_GRANTED

        coEvery { mockGeofenceClient.getLocation() } coAnswers { mockLocation }
        coEvery { mockGeofenceClient.getRegions(mockLocation) } coAnswers { mockRegions }
        coEvery { mockGeofenceClient.addGeofences(mockGeofences, mockPendingIntent) } just runs
        every { mockGeofenceClient.getGeofencingRequestBuilder() } returns mockk(relaxed = true)
        coEvery {
            mockGeofenceClient.buildGeofenceList(
                mockRegions,
                any()
            )
        } coAnswers { mockGeofences }

        val result = kotlin.runCatching { geofenceRegistry.startGeofencing(10) }.getOrNull()

        coVerify { mockGeofenceClient.addGeofences(any(), any()) }
        coVerify { geofenceScheduler.scheduleRefreshGeofencesPeriodicWorker(any(), any(), any()) }
        Truth.assertThat(result).isInstanceOf(GeoStatus.GeoStartedOk::class.java)
    }

    @Test
    fun `startGeofencing should return GeoLocationPermissionsNotGranted when permissions are missing`() =
        runTest {
            mockkStatic(ContextCompat::class)
            every {
                ContextCompat.checkSelfPermission(
                    any(),
                    any()
                )
            } returns PackageManager.PERMISSION_DENIED

            val result = geofenceRegistry.startGeofencing(10)

            assert(result is GeoStatus.GeoLocationPermissionsNotGranted)
        }
}
