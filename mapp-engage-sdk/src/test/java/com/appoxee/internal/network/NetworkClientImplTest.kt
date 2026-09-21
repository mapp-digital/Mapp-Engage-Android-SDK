package com.appoxee.internal.network

import android.util.Log
import com.appoxee.internal.model.request.GetDevice
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.network.exceptions.ClientException
import com.appoxee.internal.network.exceptions.RedirectException
import com.appoxee.internal.network.exceptions.ServerException
import com.appoxee.internal.network.response.BaseAdapter
import com.appoxee.internal.storage.PrefsStorageImpl
import com.appoxee.shared.AppoxeeOptions
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

internal class NetworkClientImplTest {
    private val devicePathV3 = "api/v3/device"
    private val inboxPathV5 = "api/v5/device/inapp/inbox"
    private val inappPathV5 = "api/v5/device/nativeinapp"
    private val inappEventsPathV5 = "api/v5/device/inapp/tracking"
    private val pushEventsPath = "/api/push/event"

    private lateinit var mockWebServer: MockWebServer
    private lateinit var networkClient: NetworkClientImpl

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } answers { 0 }
        every { Log.e(any(), any(), any()) } answers { 0 }
        every { Log.w(any(), any(), any()) } answers { 0 }
        every { Log.i(any(), any(), any()) } answers { 0 }
        every { Log.v(any(), any()) } answers { 0 }

        mockWebServer = MockWebServer()
        mockWebServer.start()
        val baseUrl = mockWebServer.url("/").toString().removeSuffix("/")

        val options = mockk<AppoxeeOptions>() {
            every { server } returns AppoxeeOptions.Server.L3
            every { sdkKey } returns "1234567.890"
            every { appId } returns "123456"
            every { tenantId } returns "7890"
            every { readTimeout } returns 2000
            every { connectionTimeout } returns 2000
            every { server.value } returns baseUrl
            every { server.internalCepUrl } returns baseUrl
        }

        val storage = mockkClass(PrefsStorageImpl::class) {
            coEvery { getInitOptions() } coAnswers { options }
        }

        networkClient = spyk(NetworkClientImpl(storage))
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        unmockkAll()
    }

    @Test
    fun `retry metadata in HTTP and application errors retries identical request`() = runTest {
        for (status in listOf(200, 404)) {
            val before = mockWebServer.requestCount
            mockWebServer.enqueue(MockResponse().setResponseCode(status).setBody(retryError(true)))
            mockWebServer.enqueue(MockResponse().setBody(MockData.GET_DEVICE_RESPONSE))

            val response = networkClient.execute(
                Request.Put(path = devicePathV3, requestBody = GetDevice()),
                BaseAdapter { DevicePayload.fromJSON(it) }
            )

            Truth.assertThat(response.isSuccess()).isTrue()
            Truth.assertThat(mockWebServer.requestCount - before).isEqualTo(2)
            val first = mockWebServer.takeRequest()
            val second = mockWebServer.takeRequest()
            Truth.assertThat(second.path).isEqualTo(first.path)
            Truth.assertThat(second.body.readUtf8()).isEqualTo(first.body.readUtf8())
        }
    }

    @Test
    fun `retry exhaustion returns metadata error without parsing empty payload`() = runTest {
        repeat(4) { mockWebServer.enqueue(MockResponse().setBody(retryError(true))) }
        val response = networkClient.execute(
            Request.Put(path = devicePathV3, requestBody = GetDevice()),
            BaseAdapter { error("Error payload must not be parsed") }
        )
        Truth.assertThat(response.isSuccess()).isFalse()
        Truth.assertThat(response.error?.message).isEqualTo("PhoneData not found")
        Truth.assertThat(mockWebServer.requestCount).isEqualTo(4)
        // Three retries wait 1s, 2s, and 4s before the final response.
        Truth.assertThat(testScheduler.currentTime).isEqualTo(7000L)
    }

    @Test
    fun `error without retry permission returns immediately`() = runTest {
        for (body in listOf(retryError(false), retryError(false).replace(", \"shouldRetry\": false", ""))) {
            mockWebServer.enqueue(MockResponse().setBody(body))
            val response = networkClient.execute(
                Request.Put(path = devicePathV3, requestBody = GetDevice()),
                BaseAdapter { error("Error payload must not be parsed") }
            )
            Truth.assertThat(response.isSuccess()).isFalse()
        }
        Truth.assertThat(mockWebServer.requestCount).isEqualTo(2)
        Truth.assertThat(testScheduler.currentTime).isEqualTo(0L)
    }

    private fun retryError(shouldRetry: Boolean) =
        """{"metadata":{"error":true,"statusCode":404,"errorMessage":"PhoneData not found", "shouldRetry": $shouldRetry},"payload":{}}"""

    @Test
    fun `test execute request and return response status`() {
        runTest {
            mockWebServer.enqueue(
                MockResponse().setResponseCode(200).setBody(MockData.GET_DEVICE_RESPONSE)
            )

            val request = Request.Put(path = devicePathV3, requestBody = GetDevice())
                .addHeader(mapOf("sdkKey" to "1232434.2343423")).setPathType(Request.PathType.BASE)

            val adapter = BaseAdapter {
                DevicePayload.fromJSON(it)
            }


            val response = networkClient.execute(request, adapter)

            val recordedRequest = mockWebServer.takeRequest()

            Truth.assertThat(response.data).isNotNull()

            Truth.assertThat(response.isSuccess()).isTrue()

            Truth.assertThat(recordedRequest.method).isEqualTo(Request.Method.PUT.name)
        }
    }

    @Test
    fun `test execute request and return response body`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(MockData.GET_DEVICE_RESPONSE)
        )

        val request = Request.Put(path = "api/v3/device", requestBody = GetDevice())
            .addHeader(mapOf("sdkKey" to "1232434.2343423")).setPathType(Request.PathType.BASE)

        val adapter = BaseAdapter {
            DevicePayload.fromJSON(it.getJSONObject("get"))
        }

        val response = networkClient.execute(request, adapter)

        val responseData = response.data

        val recordedRequest = mockWebServer.takeRequest()

        coVerify { networkClient.execute(request, adapter) }

        Truth.assertThat(responseData?.payload).isNotNull()

        Truth.assertThat(response.statusCode).isEqualTo(200)

        Truth.assertThat(recordedRequest.method).isEqualTo(Request.Method.PUT.name)
    }

    @Test
    fun `test execute request and return server exception status`() {
        runTest {
            mockWebServer.enqueue(
                MockResponse().setResponseCode(500)
            )

            val request = Request.Put(path = devicePathV3, requestBody = GetDevice())
                .addHeader(mapOf("sdkKey" to "1232434.2343423")).setPathType(Request.PathType.BASE)

            val adapter = BaseAdapter {
                DevicePayload.fromJSON(it)
            }

            try {
                networkClient.execute(request, adapter)
            } catch (e: Exception) {
                val recordedRequest = mockWebServer.takeRequest()
                Truth.assertThat(e).isInstanceOf(ServerException::class.java)
                Truth.assertThat(recordedRequest.method).isEqualTo(Request.Method.PUT.name)
            }
        }
    }

    @Test
    fun `test execute request and return client exception status`() {
        runTest {
            mockWebServer.enqueue(
                MockResponse().setResponseCode(400)
            )

            val request = Request.Put(path = devicePathV3, requestBody = GetDevice())
                .addHeader(mapOf("sdkKey" to "1232434.2343423")).setPathType(Request.PathType.BASE)

            val adapter = BaseAdapter {
                DevicePayload.fromJSON(it)
            }

            try {
                networkClient.execute(request, adapter)
            } catch (e: Exception) {
                val recordedRequest = mockWebServer.takeRequest()
                Truth.assertThat(e).isInstanceOf(ClientException::class.java)
                Truth.assertThat(recordedRequest.method).isEqualTo(Request.Method.PUT.name)
            }
        }
    }

    @Test
    fun `test execute request and return redirect exception status`() {
        runTest {
            mockWebServer.enqueue(
                MockResponse().setResponseCode(300)
            )

            val request = Request.Put(path = devicePathV3, requestBody = GetDevice())
                .addHeader(mapOf("sdkKey" to "1232434.2343423")).setPathType(Request.PathType.BASE)

            val adapter = BaseAdapter {
                DevicePayload.fromJSON(it)
            }

            try {
                networkClient.execute(request, adapter)
            } catch (e: Exception) {
                val recordedRequest = mockWebServer.takeRequest()
                Truth.assertThat(e).isInstanceOf(RedirectException::class.java)
                Truth.assertThat(recordedRequest.method).isEqualTo(Request.Method.PUT.name)
            }
        }
    }

    @Test
    fun `test execute request and return socket exception`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_DURING_REQUEST_BODY)
        )

        val request = Request.Put(path = devicePathV3, requestBody = GetDevice())
            .addHeader(mapOf("sdkKey" to "1232434.2343423")).setPathType(Request.PathType.BASE)

        val adapter = BaseAdapter {
            DevicePayload.fromJSON(it)
        }

        try {
            networkClient.execute(request, adapter)
        } catch (e: Exception) {
            val recordedRequest = mockWebServer.takeRequest()
            Truth.assertThat(e).isNotNull()
            Truth.assertThat(e).isInstanceOf(IOException::class.java)
            Truth.assertThat(recordedRequest.method).isEqualTo(Request.Method.PUT.name)
        }
    }
}
