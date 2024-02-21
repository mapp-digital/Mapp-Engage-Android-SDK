package com.appoxee.internal.network

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
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.SocketException

internal class NetworkClientImplTest {
    private val devicePathV3 = "api/v3/device"
    private val inboxPathV5 = "api/v5/device/inapp/inbox"
    private val inappPathV5 = "api/v5/device/nativeinapp"
    private val inappEventsPathV5 = "api/v5/device/inapp/tracking"
    private val pushEventsPath = "/api/push/event"

    private lateinit var server: MockWebServer
    private lateinit var networkClient: NetworkClientImpl

    @Before
    fun setUp() {
        val options = mockk<AppoxeeOptions>() {
            every { server } returns AppoxeeOptions.Server.L3
            every { sdkKey } returns "1234567.890"
            every { appId } returns "123456"
            every { tenantId } returns "7890"
            every { readTimeout } returns 10000
            every { connectionTimeout } returns 10000
            every { server.value } returns "http://127.0.0.1:8080"
            every { server.internalCepUrl } returns "http://127.0.0.1:8080"
        }

        val storage = mockkClass(PrefsStorageImpl::class) {
            coEvery { getInitOptions() } coAnswers { options }
        }

        networkClient = spyk(NetworkClientImpl(storage))
        server = MockWebServer()
        server.start(8080)
    }

    @After
    fun tearDown() {
        server.close()
        server.shutdown()
        unmockkAll()
    }

    @Test
    fun `test execute request and return response status`() {
        runBlocking {
            server.enqueue(
                MockResponse().setResponseCode(200).setBody(MockData.GET_DEVICE_RESPONSE)
            )

            val request = Request.Put(path = devicePathV3, requestBody = GetDevice())
                .addHeader(mapOf("sdkKey" to "1232434.2343423"))
                .setPathType(Request.PathType.BASE)

            val adapter = BaseAdapter {
                DevicePayload.fromJSON(it)
            }


            val response = networkClient.execute(request, adapter)

            val recordedRequest = server.takeRequest()

            Truth.assertThat(response.data).isNotNull()

            Truth.assertThat(response.isSuccess()).isTrue()

            Truth.assertThat(recordedRequest.method).isEqualTo(Request.Method.PUT.name)
        }
    }

    @Test
    fun `test execute request and return response body`() {
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(MockData.GET_DEVICE_RESPONSE)
            )

            val request = Request.Put(path = "api/v3/device", requestBody = GetDevice())
                .addHeader(mapOf("sdkKey" to "1232434.2343423"))
                .setPathType(Request.PathType.BASE)

            val adapter = BaseAdapter {
                DevicePayload.fromJSON(it.getJSONObject("get"))
            }

            val response = networkClient.execute(request, adapter)

            val responseData = response.data

            val recordedRequest = server.takeRequest()

            coVerify { networkClient.execute(request, adapter) }

            Truth.assertThat(responseData?.payload).isNotNull()

            Truth.assertThat(response.statusCode).isEqualTo(200)

            Truth.assertThat(recordedRequest.method).isEqualTo(Request.Method.PUT.name)
        }
    }

    @Test
    fun `test execute request and return server exception status`() {
        runBlocking {
            server.enqueue(
                MockResponse().setResponseCode(500)
            )

            val request = Request.Put(path = devicePathV3, requestBody = GetDevice())
                .addHeader(mapOf("sdkKey" to "1232434.2343423"))
                .setPathType(Request.PathType.BASE)

            val adapter = BaseAdapter {
                DevicePayload.fromJSON(it)
            }

            try {
                networkClient.execute(request, adapter)
            } catch (e: Exception) {
                val recordedRequest = server.takeRequest()
                Truth.assertThat(e).isInstanceOf(ServerException::class.java)
                Truth.assertThat(recordedRequest.method).isEqualTo(Request.Method.PUT.name)
            }
        }
    }

    @Test
    fun `test execute request and return client exception status`() {
        runBlocking {
            server.enqueue(
                MockResponse().setResponseCode(400)
            )

            val request = Request.Put(path = devicePathV3, requestBody = GetDevice())
                .addHeader(mapOf("sdkKey" to "1232434.2343423"))
                .setPathType(Request.PathType.BASE)

            val adapter = BaseAdapter {
                DevicePayload.fromJSON(it)
            }

            try {
                networkClient.execute(request, adapter)
            } catch (e: Exception) {
                val recordedRequest = server.takeRequest()
                Truth.assertThat(e).isInstanceOf(ClientException::class.java)
                Truth.assertThat(recordedRequest.method).isEqualTo(Request.Method.PUT.name)
            }
        }
    }

    @Test
    fun `test execute request and return redirect exception status`() {
        runBlocking {
            server.enqueue(
                MockResponse().setResponseCode(300)
            )

            val request = Request.Put(path = devicePathV3, requestBody = GetDevice())
                .addHeader(mapOf("sdkKey" to "1232434.2343423"))
                .setPathType(Request.PathType.BASE)

            val adapter = BaseAdapter {
                DevicePayload.fromJSON(it)
            }

            try {
                networkClient.execute(request, adapter)
            } catch (e: Exception) {
                val recordedRequest = server.takeRequest()
                Truth.assertThat(e).isInstanceOf(RedirectException::class.java)
                Truth.assertThat(recordedRequest.method).isEqualTo(Request.Method.PUT.name)
            }
        }
    }

    @Test
    fun `test execute request and return socket exception`() {
        runBlocking {
            server.enqueue(
                MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)
            )

            val request = Request.Put(path = devicePathV3, requestBody = GetDevice())
                .addHeader(mapOf("sdkKey" to "1232434.2343423"))
                .setPathType(Request.PathType.BASE)

            val adapter = BaseAdapter {
                DevicePayload.fromJSON(it)
            }

            try {
                networkClient.execute(request, adapter)
            } catch (e: Exception) {
                val recordedRequest = server.takeRequest()
                Truth.assertThat(e).isInstanceOf(SocketException::class.java)
                Truth.assertThat(recordedRequest.method).isEqualTo(Request.Method.PUT.name)
            }
        }
    }
}