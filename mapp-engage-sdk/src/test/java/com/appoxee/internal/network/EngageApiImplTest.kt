package com.appoxee.internal.network

import com.appoxee.internal.model.request.RegisterDevice
import com.appoxee.internal.model.request.events.ClickActionType
import com.appoxee.internal.model.request.events.PushEventType
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.request.geo.GeoEvent
import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.DefaultResponse
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.Metadata
import com.appoxee.internal.model.response.RegisterPayload
import com.appoxee.internal.model.response.ResponseData
import com.appoxee.internal.model.response.geo.RegionsResponse
import com.appoxee.internal.network.exceptions.DeviceNotRegisteredException
import com.appoxee.internal.network.response.BaseAdapter
import com.appoxee.internal.network.response.InappAdapter
import com.appoxee.internal.network.response.InboxAdapter
import com.appoxee.internal.network.response.Response
import com.appoxee.internal.network.response.ResponseAdapter
import com.appoxee.internal.provider.DeviceProvider
import com.appoxee.internal.provider.DeviceProviderImpl
import com.appoxee.shared.AppoxeeOptions
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.SocketTimeoutException
import java.util.UUID

internal class EngageApiImplTest {
    private lateinit var engageApi: EngageApi

    private lateinit var networkClient: NetworkClient

    private lateinit var options: AppoxeeOptions

    private lateinit var deviceProvider: DeviceProvider

    private lateinit var registerDevice: RegisterDevice

    private lateinit var request: Request

    private lateinit var registerPayload: RegisterPayload

    private lateinit var metadata: Metadata

    @Before
    fun setUp() {
        mockkConstructor(Request.Put::class)
        mockkConstructor(Response.Error::class)

        request = mockk() {
            every { anyConstructed<Request.Put>().path } answers { "/v3/device" }
        }

        registerPayload = mockk() {
            every { dmcUserId } returns "24342309"
            every { alias } returns "user1@mapp.com"
            every { toJSON() } answers { callOriginal() }
        }

        metadata = mockk() {
            every { statusCode } returns 200
            every { error } returns false
        }

        options = mockk(relaxed = true) {
            every { appId } returns "603123"
            every { tenantId } returns "2345"
            every { server } returns AppoxeeOptions.Server.TEST
            every { sdkKey } returns "23490834290328.3432434"
        }

        networkClient = mockk(relaxed = true)

        deviceProvider = mockk<DeviceProviderImpl>(relaxed = true) {
            every { getOSName() } answers { "Android" }
            every { getOSNumber() } answers { "14" }
            every { getAppVersion() } answers { "1.2.3" }
            every { getClientVersion() } answers { "1.0.0" }
            every { getLocale() } answers { "en" }
            every { getTimeZone() } answers { "Belgrade/Serbia" }
            every { getHardwareType() } answers { "Samsung s23 Ultra" }
            every { getDensity() } answers { "300dpi" }
            every { getResolution() } answers { "2160x1440" }
            every { getVendorId() } answers { "Samsung" }
            every { getUniqueDeviceId() } answers { UUID.randomUUID().toString() }
            every { generateRegistrationDevice() } answers { callOriginal() }
        }

        registerDevice = mockk(relaxed = true) {
            every { deviceProvider.generateRegistrationDevice() } answers { callOriginal() }
        }

        engageApi = mockk()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `register devices successful`() = runBlocking {
        val registerAdapter = mockk<BaseAdapter<RegisterPayload>> {}

        coEvery { networkClient.execute(request, registerAdapter) } coAnswers {
            Response.success(200, ResponseData(metadata = metadata, payload = registerPayload))
        }

        coEvery { engageApi.registerDevice(registerDevice) } coAnswers {
            networkClient.execute(
                request, registerAdapter
            )
        }

        val apiResponse = engageApi.registerDevice(registerDevice)
        Truth.assertThat(apiResponse.isSuccess()).isTrue()
        Truth.assertThat(apiResponse.data).isNotNull()
        Truth.assertThat(apiResponse.error).isNull()

        coVerify(atLeast = 1) { engageApi.registerDevice(registerDevice) }
        coVerify(atLeast = 1) { networkClient.execute(request, registerAdapter) }
    }

    @Test
    fun `register device error`() = runBlocking {
        val registerAdapter = mockk<BaseAdapter<RegisterPayload>>()

        coEvery { networkClient.execute(request, registerAdapter) } coAnswers {
            Response.error(DeviceNotRegisteredException())
        }

        coEvery { engageApi.registerDevice(registerDevice) } coAnswers {
            networkClient.execute(
                request, registerAdapter
            )
        }

        val apiResponse = engageApi.registerDevice(registerDevice)
        Truth.assertThat(apiResponse.isSuccess()).isFalse()
        Truth.assertThat(apiResponse.error).isNotNull()
        coVerify(atLeast = 1) { engageApi.registerDevice(registerDevice) }
        coVerify(atLeast = 1) { networkClient.execute(request, registerAdapter) }
    }

    @Test
    fun `get device successful`() = runBlocking {
        val adapter = mockk<BaseAdapter<DevicePayload>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.success(
                200, ResponseData(metadata = metadata, payload = mockk(relaxed = true))
            )
        }

        coEvery { engageApi.getDevice() } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.getDevice()
        Truth.assertThat(apiResponse.isSuccess()).isTrue()
        Truth.assertThat(apiResponse.data).isNotNull()
        Truth.assertThat(apiResponse.error).isNull()

        coVerify(atLeast = 1) { engageApi.getDevice() }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `get device error`() = runBlocking {
        val adapter = mockk<BaseAdapter<DevicePayload>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.error(SocketTimeoutException())
        }

        coEvery { engageApi.getDevice() } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.getDevice()
        Truth.assertThat(apiResponse.isSuccess()).isFalse()
        Truth.assertThat(apiResponse.data).isNull()
        Truth.assertThat(apiResponse.error).isNotNull()

        coVerify(atLeast = 1) { engageApi.getDevice() }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `activate successful`() = runBlocking {
        val adapter = mockk<BaseAdapter<DefaultResponse>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.success(
                200, ResponseData(metadata = metadata, payload = mockk(relaxed = true))
            )
        }

        coEvery { engageApi.activate(2000) } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.activate(2000)
        Truth.assertThat(apiResponse.isSuccess()).isTrue()
        Truth.assertThat(apiResponse.data).isNotNull()
        Truth.assertThat(apiResponse.error).isNull()

        coVerify(atLeast = 1) { engageApi.activate(2000) }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `activate error`() = runBlocking {
        val adapter = mockk<BaseAdapter<DefaultResponse>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.error(SocketTimeoutException())
        }

        coEvery { engageApi.activate(2000) } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.activate(2000)
        Truth.assertThat(apiResponse.isSuccess()).isFalse()
        Truth.assertThat(apiResponse.data).isNull()
        Truth.assertThat(apiResponse.error).isNotNull()

        coVerify(atLeast = 1) { engageApi.activate(2000) }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `set alias successful`() = runBlocking {
        val adapter = mockk<BaseAdapter<DefaultResponse>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.success(
                200, ResponseData(metadata = metadata, payload = mockk(relaxed = true))
            )
        }

        coEvery { engageApi.setAlias("user2@mapp.com") } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.setAlias("user2@mapp.com")
        Truth.assertThat(apiResponse.isSuccess()).isTrue()
        Truth.assertThat(apiResponse.data).isNotNull()
        Truth.assertThat(apiResponse.error).isNull()

        coVerify(atLeast = 1) { engageApi.setAlias("user2@mapp.com") }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `set alias error`() = runBlocking {
        val adapter = mockk<BaseAdapter<DefaultResponse>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.error(SocketTimeoutException())
        }

        coEvery { engageApi.setAlias("user2@mapp.com") } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.setAlias("user2@mapp.com")
        Truth.assertThat(apiResponse.isSuccess()).isFalse()
        Truth.assertThat(apiResponse.data).isNull()
        Truth.assertThat(apiResponse.error).isNotNull()

        coVerify(atLeast = 1) { engageApi.setAlias("user2@mapp.com") }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `get alias successful`() = runBlocking {
        val adapter = mockk<BaseAdapter<DevicePayload>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.success(
                200, ResponseData(metadata = metadata, payload = mockk(relaxed = true))
            )
        }

        coEvery { engageApi.getAlias() } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.getAlias()
        Truth.assertThat(apiResponse.isSuccess()).isTrue()
        Truth.assertThat(apiResponse.data).isNotNull()
        Truth.assertThat(apiResponse.error).isNull()

        coVerify(atLeast = 1) { engageApi.getAlias() }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `get alias error`() = runBlocking {
        val adapter = mockk<BaseAdapter<DevicePayload>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.error(SocketTimeoutException())
        }

        coEvery { engageApi.getAlias() } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.getAlias()
        Truth.assertThat(apiResponse.isSuccess()).isFalse()
        Truth.assertThat(apiResponse.data).isNull()
        Truth.assertThat(apiResponse.error).isNotNull()

        coVerify(atLeast = 1) { engageApi.getAlias() }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `optIn successful`() = runBlocking {
        val adapter = mockk<BaseAdapter<DefaultResponse>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.success(
                200, ResponseData(metadata = metadata, payload = mockk(relaxed = true))
            )
        }

        coEvery { engageApi.optIn("1234") } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.optIn("1234")
        Truth.assertThat(apiResponse.isSuccess()).isTrue()
        Truth.assertThat(apiResponse.data).isNotNull()
        Truth.assertThat(apiResponse.error).isNull()

        coVerify(atLeast = 1) { engageApi.optIn("1234") }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `optIn error`() = runBlocking {
        val adapter = mockk<BaseAdapter<DefaultResponse>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.error(SocketTimeoutException())
        }

        coEvery { engageApi.optIn("") } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.optIn("")
        Truth.assertThat(apiResponse.isSuccess()).isFalse()
        Truth.assertThat(apiResponse.data).isNull()
        Truth.assertThat(apiResponse.error).isNotNull()

        coVerify(atLeast = 1) { engageApi.optIn("") }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `optOut successful`() = runBlocking {
        val adapter = mockk<BaseAdapter<DefaultResponse>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.success(
                200, ResponseData(metadata = metadata, payload = mockk(relaxed = true))
            )
        }

        coEvery { engageApi.optOut("1234") } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.optOut("1234")
        Truth.assertThat(apiResponse.isSuccess()).isTrue()
        Truth.assertThat(apiResponse.data).isNotNull()
        Truth.assertThat(apiResponse.error).isNull()

        coVerify(atLeast = 1) { engageApi.optOut("1234") }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `optOut error`() = runBlocking {
        val adapter = mockk<BaseAdapter<DefaultResponse>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.error(SocketTimeoutException())
        }

        coEvery { engageApi.optOut("") } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.optOut("")
        Truth.assertThat(apiResponse.isSuccess()).isFalse()
        Truth.assertThat(apiResponse.data).isNull()
        Truth.assertThat(apiResponse.error).isNotNull()

        coVerify(atLeast = 1) { engageApi.optOut("") }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `get App Config successful`() = runBlocking {
        val adapter = mockk<BaseAdapter<AppConfigPayload>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.success(
                200, ResponseData(metadata = metadata, payload = mockk(relaxed = true))
            )
        }

        coEvery { engageApi.getAppConfig() } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.getAppConfig()
        Truth.assertThat(apiResponse.isSuccess()).isTrue()
        Truth.assertThat(apiResponse.data).isNotNull()
        Truth.assertThat(apiResponse.error).isNull()

        coVerify(atLeast = 1) { engageApi.getAppConfig() }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `get App Config error`() = runBlocking {
        val adapter = mockk<BaseAdapter<AppConfigPayload>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.error(SocketTimeoutException())
        }

        coEvery { engageApi.getAppConfig() } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.getAppConfig()
        Truth.assertThat(apiResponse.isSuccess()).isFalse()
        Truth.assertThat(apiResponse.data).isNull()
        Truth.assertThat(apiResponse.error).isNotNull()

        coVerify(atLeast = 1) { engageApi.getAppConfig() }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `fetch inbox messages successful`() = runBlocking {
        val adapter = mockk<InboxAdapter> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.success(200, mockk(relaxed = true))
        }

        coEvery { engageApi.fetchInboxMessages("app_open") } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.fetchInboxMessages("app_open")
        Truth.assertThat(apiResponse.isSuccess()).isTrue()
        Truth.assertThat(apiResponse.data).isNotNull()
        Truth.assertThat(apiResponse.error).isNull()

        coVerify(atLeast = 1) { engageApi.fetchInboxMessages("app_open") }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `fetch inbox messages error`() = runBlocking {
        val adapter = mockk<InboxAdapter> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.error(SocketTimeoutException())
        }

        coEvery { engageApi.fetchInboxMessages("") } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.fetchInboxMessages("")
        Truth.assertThat(apiResponse.isSuccess()).isFalse()
        Truth.assertThat(apiResponse.data).isNull()
        Truth.assertThat(apiResponse.error).isNotNull()

        coVerify(atLeast = 1) { engageApi.fetchInboxMessages("") }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `fetch inapp successful`() = runBlocking {
        val adapter = mockk<InappAdapter> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.success(200, mockk(relaxed = true))
        }

        coEvery { engageApi.fetchInApp("app_open") } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.fetchInApp("app_open")
        Truth.assertThat(apiResponse.isSuccess()).isTrue()
        Truth.assertThat(apiResponse.data).isNotNull()
        Truth.assertThat(apiResponse.error).isNull()

        coVerify(atLeast = 1) { engageApi.fetchInApp("app_open") }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `fetch inapp error`() = runBlocking {
        val adapter = mockk<InappAdapter> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.error(SocketTimeoutException())
        }

        coEvery { engageApi.fetchInApp("") } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.fetchInApp("")
        Truth.assertThat(apiResponse.isSuccess()).isFalse()
        Truth.assertThat(apiResponse.data).isNull()
        Truth.assertThat(apiResponse.error).isNotNull()

        coVerify(atLeast = 1) { engageApi.fetchInApp("") }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `add tags successful`() = runBlocking {
        val adapter = mockk<BaseAdapter<DefaultResponse>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.success(
                200, ResponseData(metadata = metadata, payload = mockk(relaxed = true))
            )
        }

        coEvery { engageApi.addTags(emptyList()) } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.addTags(emptyList())
        Truth.assertThat(apiResponse.isSuccess()).isTrue()
        Truth.assertThat(apiResponse.data).isNotNull()
        Truth.assertThat(apiResponse.error).isNull()

        coVerify(atLeast = 1) { engageApi.addTags(emptyList()) }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `add tags error`() = runBlocking {
        val adapter = mockk<BaseAdapter<DefaultResponse>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.error(SocketTimeoutException())
        }

        coEvery { engageApi.addTags(emptyList()) } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.addTags(emptyList())
        Truth.assertThat(apiResponse.isSuccess()).isFalse()
        Truth.assertThat(apiResponse.data).isNull()
        Truth.assertThat(apiResponse.error).isNotNull()

        coVerify(atLeast = 1) { engageApi.addTags(emptyList()) }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `remove tags successful`() = runBlocking {
        val adapter = mockk<BaseAdapter<DefaultResponse>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.success(
                200, ResponseData(metadata = metadata, payload = mockk(relaxed = true))
            )
        }

        coEvery { engageApi.removeTags(emptyList()) } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.removeTags(emptyList())
        Truth.assertThat(apiResponse.isSuccess()).isTrue()
        Truth.assertThat(apiResponse.data).isNotNull()
        Truth.assertThat(apiResponse.error).isNull()

        coVerify(atLeast = 1) { engageApi.removeTags(emptyList()) }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `remove tags error`() = runBlocking {
        val adapter = mockk<BaseAdapter<DefaultResponse>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.error(SocketTimeoutException())
        }

        coEvery { engageApi.removeTags(emptyList()) } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.removeTags(emptyList())
        Truth.assertThat(apiResponse.isSuccess()).isFalse()
        Truth.assertThat(apiResponse.data).isNull()
        Truth.assertThat(apiResponse.error).isNotNull()

        coVerify(atLeast = 1) { engageApi.removeTags(emptyList()) }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `add custom attributes successful`() = runBlocking {
        val adapter = mockk<BaseAdapter<DefaultResponse>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.success(
                200, ResponseData(metadata = metadata, payload = mockk(relaxed = true))
            )
        }

        coEvery { engageApi.addCustomAttributes(emptyMap()) } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.addCustomAttributes(emptyMap())
        Truth.assertThat(apiResponse.isSuccess()).isTrue()
        Truth.assertThat(apiResponse.data).isNotNull()
        Truth.assertThat(apiResponse.error).isNull()

        coVerify(atLeast = 1) { engageApi.addCustomAttributes(emptyMap()) }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `add custom attributes error`() = runBlocking {
        val adapter = mockk<BaseAdapter<DefaultResponse>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.error(SocketTimeoutException())
        }

        coEvery { engageApi.addCustomAttributes(emptyMap()) } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.addCustomAttributes(emptyMap())
        Truth.assertThat(apiResponse.isSuccess()).isFalse()
        Truth.assertThat(apiResponse.data).isNull()
        Truth.assertThat(apiResponse.error).isNotNull()

        coVerify(atLeast = 1) { engageApi.addCustomAttributes(emptyMap()) }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `get custom attributes successful`() = runBlocking {
        val adapter = mockk<BaseAdapter<Map<String, Any?>>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.success(
                200, ResponseData(metadata = metadata, payload = mockk(relaxed = true))
            )
        }

        coEvery { engageApi.getCustomAttributes(emptyList()) } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.getCustomAttributes(emptyList())
        Truth.assertThat(apiResponse.isSuccess()).isTrue()
        Truth.assertThat(apiResponse.data).isNotNull()
        Truth.assertThat(apiResponse.error).isNull()

        coVerify(atLeast = 1) { engageApi.getCustomAttributes(emptyList()) }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `get custom attributes error`() = runBlocking {
        val adapter = mockk<BaseAdapter<Map<String, Any?>>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.error(SocketTimeoutException())
        }

        coEvery { engageApi.getCustomAttributes(emptyList()) } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.getCustomAttributes(emptyList())
        Truth.assertThat(apiResponse.isSuccess()).isFalse()
        Truth.assertThat(apiResponse.data).isNull()
        Truth.assertThat(apiResponse.error).isNotNull()

        coVerify(atLeast = 1) { engageApi.getCustomAttributes(emptyList()) }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `send inapp event successful`() = runBlocking {
        val adapter = mockk<BaseAdapter<Boolean>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.success(
                200, ResponseData(metadata = metadata, payload = mockk(relaxed = true))
            )
        }

        coEvery { engageApi.inappEvent("1234", 1234, TrackingKey.IA_MSG_DISPLAYED) } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.inappEvent("1234", 1234, TrackingKey.IA_MSG_DISPLAYED)
        Truth.assertThat(apiResponse.isSuccess()).isTrue()
        Truth.assertThat(apiResponse.data).isNotNull()
        Truth.assertThat(apiResponse.error).isNull()

        coVerify(atLeast = 1) { engageApi.inappEvent("1234", 1234, TrackingKey.IA_MSG_DISPLAYED) }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `send inapp event error`() = runBlocking {
        val adapter = mockk<BaseAdapter<Boolean>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.error(SocketTimeoutException())
        }

        coEvery { engageApi.inappEvent("1234", 1234, TrackingKey.IA_MSG_DISPLAYED) } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.inappEvent("1234", 1234, TrackingKey.IA_MSG_DISPLAYED)
        Truth.assertThat(apiResponse.isSuccess()).isFalse()
        Truth.assertThat(apiResponse.data).isNull()
        Truth.assertThat(apiResponse.error).isNotNull()

        coVerify(atLeast = 1) { engageApi.inappEvent("1234", 1234, TrackingKey.IA_MSG_DISPLAYED) }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `send push event successful`() = runBlocking {
        val adapter = mockk<BaseAdapter<Boolean>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.success(
                200, ResponseData(metadata = metadata, payload = mockk(relaxed = true))
            )
        }

        coEvery {
            engageApi.pushEvent(
                1234, 1, ClickActionType.DIAL_NUMBER, PushEventType.CLICK
            )
        } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse =
            engageApi.pushEvent(1234, 1, ClickActionType.DIAL_NUMBER, PushEventType.CLICK)
        Truth.assertThat(apiResponse.isSuccess()).isTrue()
        Truth.assertThat(apiResponse.data).isNotNull()
        Truth.assertThat(apiResponse.error).isNull()

        coVerify(atLeast = 1) {
            engageApi.pushEvent(
                1234, 1, ClickActionType.DIAL_NUMBER, PushEventType.CLICK
            )
        }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `send push event error`() = runBlocking {
        val adapter = mockk<BaseAdapter<Boolean>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.error(SocketTimeoutException())
        }

        coEvery {
            engageApi.pushEvent(
                1234, 1, ClickActionType.DIAL_NUMBER, PushEventType.CLICK
            )
        } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse =
            engageApi.pushEvent(1234, 1, ClickActionType.DIAL_NUMBER, PushEventType.CLICK)
        Truth.assertThat(apiResponse.isSuccess()).isFalse()
        Truth.assertThat(apiResponse.data).isNull()
        Truth.assertThat(apiResponse.error).isNotNull()

        coVerify(atLeast = 1) {
            engageApi.pushEvent(
                1234, 1, ClickActionType.DIAL_NUMBER, PushEventType.CLICK
            )
        }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `get regions successful`() = runBlocking {
        val adapter = mockk<ResponseAdapter<ResponseData<RegionsResponse>>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.success(200, mockk(relaxed = true))
        }

        coEvery { engageApi.getRegions(0.0, 0.0, 1, 20) } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.getRegions(0.0, 0.0, 1, 20)
        Truth.assertThat(apiResponse.isSuccess()).isTrue()
        Truth.assertThat(apiResponse.data).isNotNull()
        Truth.assertThat(apiResponse.error).isNull()

        coVerify(atLeast = 1) { engageApi.getRegions(0.0, 0.0, 1, 20) }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `get regions error`() = runBlocking {
        val adapter = mockk<ResponseAdapter<ResponseData<RegionsResponse>>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.error(SocketTimeoutException())
        }

        coEvery { engageApi.getRegions(0.0, 0.0, 1, 20) } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.getRegions(0.0, 0.0, 1, 20)
        Truth.assertThat(apiResponse.isSuccess()).isFalse()
        Truth.assertThat(apiResponse.data).isNull()
        Truth.assertThat(apiResponse.error).isNotNull()

        coVerify(atLeast = 1) { engageApi.getRegions(0.0, 0.0, 1, 20) }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `send regions event successful`() = runBlocking {
        val adapter = mockk<BaseAdapter<DefaultResponse>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.success(
                200, ResponseData(metadata = metadata, payload = mockk(relaxed = true))
            )
        }

        coEvery { engageApi.regionEvent(GeoEvent.ENTER, 0.0, 0.0, 50, 1) } coAnswers {
            networkClient.execute(request, adapter)
        }

        val apiResponse = engageApi.regionEvent(GeoEvent.ENTER, 0.0, 0.0, 50, 1)
        Truth.assertThat(apiResponse.isSuccess()).isTrue()
        Truth.assertThat(apiResponse.data).isNotNull()
        Truth.assertThat(apiResponse.error).isNull()

        coVerify(atLeast = 1) { engageApi.regionEvent(GeoEvent.ENTER, 0.0, 0.0, 50, 1) }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }

    @Test
    fun `send regions event error`() = runBlocking {
        val adapter = mockk<BaseAdapter<DefaultResponse>> {}

        coEvery { networkClient.execute(request, adapter) } coAnswers {
            Response.error(SocketTimeoutException())
        }

        coEvery {
            engageApi.regionEvent(GeoEvent.ENTER, 0.0, 0.0, 50, 1)
        } coAnswers {
            networkClient.execute(
                request, adapter
            )
        }

        val apiResponse = engageApi.regionEvent(GeoEvent.ENTER, 0.0, 0.0, 50, 1)
        Truth.assertThat(apiResponse.isSuccess()).isFalse()
        Truth.assertThat(apiResponse.data).isNull()
        Truth.assertThat(apiResponse.error).isNotNull()

        coVerify(atLeast = 1) { engageApi.regionEvent(GeoEvent.ENTER, 0.0, 0.0, 50, 1) }
        coVerify(atLeast = 1) { networkClient.execute(request, adapter) }
    }
}