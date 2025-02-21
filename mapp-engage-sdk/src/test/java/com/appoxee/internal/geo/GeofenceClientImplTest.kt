package com.appoxee.internal.geo

import android.app.PendingIntent
import android.content.Context
import android.location.Location
import com.appoxee.internal.model.response.ResponseData
import com.appoxee.internal.model.response.geo.Region
import com.appoxee.internal.model.response.geo.RegionsResponse
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.network.response.Response
import com.appoxee.shared.GeoStatus
import com.appoxee.shared.GeofenceException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class GeofenceClientImplTest {
    private lateinit var context: Context
    private lateinit var sut: GeofenceClient
    private lateinit var locationProvider: LocationProvider
    private lateinit var engageApi: EngageApi
    private lateinit var geofencingClient: GeofencingClient

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        locationProvider = mockk(relaxed = true)
        engageApi = mockk(relaxed = true)
        geofencingClient = mockk(relaxed = true)

        sut = spyk(GeofenceClientImpl(context, locationProvider, engageApi, geofencingClient))
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `get regions with success getting location`() = runTest {
        val mockkLocation: Location = mockk(relaxed = true)
        val regions: List<Region> = listOf(mockk(), mockk())
        val regionsResponse = RegionsResponse(1, regions)
        val successRegionsResponse = Response.success(200, ResponseData(mockk(), regionsResponse))
        every { mockkLocation.latitude } returns 20.0
        every { mockkLocation.longitude } returns 18.0
        coEvery { locationProvider.getCurrentLocation() } coAnswers { mockkLocation }
        coEvery {
            engageApi.getRegions(
                any(),
                any(),
                any(),
                any()
            )
        } coAnswers { successRegionsResponse }
        sut.getRegions(mockkLocation)

        coVerify { engageApi.getRegions(any(), any(), any(), any()) }
    }

    @Test
    fun `get regions with exception getting location`() = runTest {
        val mockkLocation: Location = mockk(relaxed = true)
        coEvery {
            engageApi.getRegions(
                any(),
                any(),
                any(),
                any()
            )
        } throws GeofenceException(GeoStatus.GeoFailedGettingRegions())
        val result = kotlin.runCatching { sut.getRegions(mockkLocation) }.exceptionOrNull()

        coVerify { engageApi.getRegions(any(), any(), any(), any()) }
        Truth.assertThat(result).isInstanceOf(GeofenceException::class.java)
    }

    @Test
    fun `get location returns success`() = runTest {
        val mockkLocation: Location = mockk(relaxed = true)
        every { mockkLocation.latitude } returns 20.0
        every { mockkLocation.longitude } returns 18.0

        coEvery { locationProvider.getCurrentLocation() } coAnswers { mockkLocation }
        val result = kotlin.runCatching { sut.getLocation() }.getOrNull()

        coVerify { locationProvider.getCurrentLocation() }
        Truth.assertThat(result).isEqualTo(mockkLocation)
    }

    @Test
    fun `get location throws exception`() = runTest {
        val mockkLocation: Location = mockk(relaxed = true)
        every { mockkLocation.latitude } returns 20.0
        every { mockkLocation.longitude } returns 18.0

        coEvery { locationProvider.getCurrentLocation() } coAnswers { null }
        val result = kotlin.runCatching { sut.getLocation() }.exceptionOrNull()

        coVerify { locationProvider.getCurrentLocation() }
        Truth.assertThat(result).isInstanceOf(GeofenceException::class.java)
    }

    @Test
    fun `add geofences with success`() = runTest {
        val geofences: List<Geofence> = listOf(mockk(relaxed = true))
        val pendingIntent: PendingIntent = mockk(relaxed = true)

        val builder: GeofencingRequest.Builder = mockk(relaxed = true)

        every { sut.getGeofencingRequestBuilder() } returns builder
        every { sut.removeGeofences(any()) } just runs

        sut.addGeofences(geofences, pendingIntent)

        coVerify { geofencingClient.addGeofences(any(), any()) }
    }

    @Test
    fun `add geofences throws exception when geofences list is empty`() = runTest {
        val geofences: List<Geofence> = emptyList()
        val pendingIntent: PendingIntent = mockk(relaxed = true)

        val builder: GeofencingRequest.Builder = mockk(relaxed = true)

        every { sut.getGeofencingRequestBuilder() } returns builder
        every { sut.removeGeofences(any()) } just runs

        val result =
            kotlin.runCatching { sut.addGeofences(geofences, pendingIntent) }.exceptionOrNull()

        coVerify(exactly = 0) { geofencingClient.addGeofences(any(), any()) }

        Truth.assertThat(result).isInstanceOf(GeofenceException::class.java)
    }
}