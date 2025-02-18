package com.appoxee.internal.geo

import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.appoxee.internal.model.response.geo.Region
import com.appoxee.internal.provider.SystemInfoProvider
import com.appoxee.shared.GeoStatus
import com.google.android.gms.location.Geofence
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.justRun
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

    @MockK
    private lateinit var mockPackageManager: PackageManager

    @MockK
    private lateinit var mockGeofenceClient: GeofenceClient

    @MockK
    private lateinit var mockPendingIntent: PendingIntent

    @MockK
    private lateinit var geofenceScheduler: GeofenceScheduler

    @MockK
    private lateinit var mockSystemInfoProvider: SystemInfoProvider

    private lateinit var geofenceRegistry: GeofenceRegistry

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        mockContext = mockk(relaxed = true)
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

        coEvery { geofenceScheduler.postGeofenceEvent(any(), any()) } just runs
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
        coEvery {
            mockGeofenceClient.buildGeofenceList(
                mockRegions,
                any()
            )
        } coAnswers { mockGeofences }
        coEvery { mockGeofenceClient.addGeofences(mockGeofences, mockPendingIntent) } just runs

        val result = geofenceRegistry.startGeofencing(10)

        assert(result is GeoStatus.GeoStartedOk)
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
