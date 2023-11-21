package com.appoxee.internal.network

import com.appoxee.internal.model.request.GetDevice
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.ResponseData
import com.appoxee.internal.network.exceptions.ServerException
import com.appoxee.internal.network.response.BaseAdapter
import com.appoxee.internal.util.convertToString
import com.appoxee.shared.AppoxeeOptions
import com.google.common.truth.Truth
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.DataOutputStream
import java.net.HttpURLConnection

internal class NetworkClientImplTest {
    private val devicePathV3 = "api/v3/device"
    private val inboxPathV5 = "api/v5/device/inapp/inbox"
    private val inappPathV5 = "api/v5/device/nativeinapp"
    private val inappEventsPathV5 = "api/v5/device/inapp/tracking"
    private val pushEventsPath = "/api/push/event"

    private lateinit var server: MockWebServer
    private lateinit var networkClient: NetworkClientImpl
    private lateinit var options: AppoxeeOptions

    @Before
    fun setUp() {
        options = spyk(AppoxeeOptions(AppoxeeOptions.Server.TEST, "1234567.890", "123456", "7890"))
        networkClient = spyk(NetworkClientImpl(options), recordPrivateCalls = true)
        server = MockWebServer()
        server.start(8080)
        every { options.server.value } returns "http://127.0.0.1:8080"
        every { options.server.internalCepUrl } returns "http://127.0.0.1:8080"
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `test execute request and return response status`() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(MockData.GET_DEVICE_RESPONSE)
        )

        val request = Request.Put(path = devicePathV3, requestBody = GetDevice())
            .addHeader(mapOf("sdkKey" to "1232434.2343423"))
            .setPathType(Request.PathType.BASE)

        val adapter = BaseAdapter {
            DevicePayload.fromJSON(it)
        }


        val response = runBlocking { networkClient.execute(request, adapter) }

        val recordedRequest = server.takeRequest()

        Truth.assertThat(response.data).isNotNull()

        Truth.assertThat(response.isSuccess()).isTrue()

        Truth.assertThat(recordedRequest.method).isEqualTo(Request.Method.PUT.name)
    }

    @Test
    fun `test execute request and return response body`() {
        runBlocking {
            val httpUrl = server.url(devicePathV3)

            val responseBodyJson = JSONObject(MockData.GET_DEVICE_RESPONSE)

            server.enqueue(
                MockResponse()
                    .setBody(responseBodyJson.toString())
                    .setResponseCode(200)
            )

            val request = Request.Put(path = "api/v3/device", requestBody = GetDevice())
                .addHeader(mapOf("sdkKey" to "1232434.2343423"))
                .setPathType(Request.PathType.BASE)

            val connection: HttpURLConnection =
                httpUrl.toUrl().openConnection() as HttpURLConnection

            connection.requestMethod = Request.Method.PUT.name
            connection.doOutput = request.doOutput

            request.requestBody?.asString()?.encodeToByteArray()?.let {
                DataOutputStream(connection.outputStream).apply {
                    write(it)
                    flush()
                    close()
                }
            }

            val json = JSONObject(connection.inputStream.convertToString())

            val responseData = ResponseData.fromJSON(json) {
                it.optJSONObject("get")?.let { payload ->
                    DevicePayload.fromJSON(payload)
                }
            }

            val recordedRequest = server.takeRequest()

            Truth.assertThat(responseData.payload).isNotNull()

            Truth.assertThat(connection.responseCode).isEqualTo(200)

            Truth.assertThat(recordedRequest.method).isEqualTo(Request.Method.PUT.name)
        }
    }

    @Test
    fun `test execute request and return server exception status`() {
        coJustRun {
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
}