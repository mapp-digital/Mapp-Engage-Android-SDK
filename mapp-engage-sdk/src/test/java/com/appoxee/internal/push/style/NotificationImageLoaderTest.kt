package com.appoxee.internal.push.style

import com.appoxee.internal.ui.push.style.NotificationImageLoader
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import org.junit.After
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationImageLoaderTest {
    private val callFactory = mockk<Call.Factory>()
    private val httpCall = mockk<Call>(relaxed = true)
    private val url = "https://images.example.com/image.png"

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `rejects non HTTPS and credential bearing media before opening a connection`() = runTest {
        val loader = NotificationImageLoader(callFactory, StandardTestDispatcher(testScheduler))

        for (path in listOf(null, "", "file:///private/image.png", "content://images/1",
            "http://images.example.com/image.png", "https://user:password@images.example.com/image.png")) {
            assertThat(loader.load(path)).isNull()
        }

        verify(exactly = 0) { callFactory.newCall(any()) }
    }

    @Test
    fun `rejects advertised and streamed oversized bodies and closes them`() = runTest {
        for (advertiseLength in listOf(true, false)) {
            var closed = false
            val bytes = ByteArray(5 * 1024 * 1024 + 1)
            val body = object : ResponseBody() {
                private val buffer = Buffer().write(bytes)
                override fun contentType(): MediaType? = null
                override fun contentLength(): Long = if (advertiseLength) bytes.size.toLong() else -1
                override fun source(): BufferedSource = buffer
                override fun close() { closed = true; super.close() }
            }
            respond(response(body = body))
            val loader = NotificationImageLoader(callFactory, StandardTestDispatcher(testScheduler))

            assertThat(loader.load(url)).isNull()
            assertThat(closed).isTrue()
        }
    }

    @Test
    fun `does not follow HTTPS redirects to HTTP`() = runTest {
        respond(response(code = 302, location = "http://images.example.com/image.png"))
        val loader = NotificationImageLoader(callFactory, StandardTestDispatcher(testScheduler))

        assertThat(loader.load(url)).isNull()

        verify(exactly = 1) { callFactory.newCall(any()) }
    }

    @Test
    fun `stops redirect loops after three redirects`() = runTest {
        respond(response(code = 302, location = url))
        val loader = NotificationImageLoader(callFactory, StandardTestDispatcher(testScheduler))

        assertThat(loader.load(url)).isNull()

        verify(exactly = 4) { callFactory.newCall(any()) }
    }

    @Test
    fun `parent cancellation cancels the underlying network call`() = runTest {
        every { callFactory.newCall(any()) } returns httpCall
        val loader = NotificationImageLoader(callFactory, StandardTestDispatcher(testScheduler))
        val job = launch { loader.load(url) }
        runCurrent()

        job.cancel()
        job.join()

        assertThat(job.isCancelled).isTrue()
        verify(exactly = 1) { httpCall.cancel() }
    }

    @Test
    fun `total time budget cancels a stalled image without failing the notification`() = runTest {
        every { callFactory.newCall(any()) } returns httpCall
        val loader = NotificationImageLoader(callFactory, StandardTestDispatcher(testScheduler))

        assertThat(loader.load(url)).isNull()

        verify(exactly = 1) { httpCall.cancel() }
        assertThat(testScheduler.currentTime).isEqualTo(20_000L)
    }

    @Test
    fun `total time budget includes waiting for an image loading slot`() = runTest {
        every { callFactory.newCall(any()) } returns httpCall
        val loader = NotificationImageLoader(callFactory, StandardTestDispatcher(testScheduler))
        val first = launch { loader.load(url) }
        val second = launch { loader.load(url) }
        runCurrent()
        advanceTimeBy(1_000)

        // Both slots remain occupied until 20 seconds. This request must still
        // finish within its own 20-second budget, including the 19-second wait.
        assertThat(loader.load(url)).isNull()

        assertThat(testScheduler.currentTime).isEqualTo(21_000L)
        first.join()
        second.join()
    }

    @Test
    fun `downsampling bounds output dimensions including rounding edges`() {
        for ((width, height) in listOf(1 to 1, 2049 to 1024, 8192 to 4096, 32768 to 1)) {
            val sample = NotificationImageLoader.sampleSize(width, height)
            assertThat((width + sample - 1) / sample).isAtMost(1024)
            assertThat((height + sample - 1) / sample).isAtMost(1024)
        }
    }

    @Test
    fun `rejects invalid dimensions and image bombs before decoding pixels`() {
        for ((width, height) in listOf(0 to 10, -1 to 10, 10 to 0, 10_000 to 10_000, Int.MAX_VALUE to 1)) {
            try {
                NotificationImageLoader.sampleSize(width, height)
                throw AssertionError("Expected invalid image dimensions to be rejected")
            } catch (expected: IOException) {
                // Expected rejection before allocating a bitmap.
            }
        }
    }

    private fun respond(response: Response) {
        every { callFactory.newCall(any()) } returns httpCall
        every { httpCall.enqueue(any()) } answers {
            firstArg<Callback>().onResponse(httpCall, response)
        }
    }

    private fun response(
        code: Int = 200,
        location: String? = null,
        body: ResponseBody = "image".toResponseBody()
    ): Response = Response.Builder()
        .request(Request.Builder().url(url).build())
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("Test response")
        .body(body)
        .apply { location?.let { header("Location", it) } }
        .build()
}
