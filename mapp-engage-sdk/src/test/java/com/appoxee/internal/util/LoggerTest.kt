package com.appoxee.internal.util

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import com.appoxee.shared.AppoxeeOptions.LogLevel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class LoggerTest {
    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any(), any()) } returns 0
        every { Log.i(any(), any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        initLogger(debuggable = false, logLevel = LogLevel.RELEASE)
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `production logging redacts payloads and exception chains at every level`() {
        val details = "token=secret email=customer@example.com https://example.com/?private=value"
        val error = IllegalStateException(details, IllegalArgumentException(details))

        Logger.d("test", details)
        Logger.w("test", details, error)
        Logger.i("test", details, error)
        Logger.e("test", details, error)
        Logger.e("test", error)

        verify(exactly = 1) { Log.d("test", "[details redacted]") }
        verify(exactly = 1) { Log.w("test", "[details redacted]", null) }
        verify(exactly = 1) { Log.i("test", "[details redacted]", null) }
        verify(exactly = 2) { Log.e("test", "[details redacted]", null) }
    }

    @Test
    fun `production logging keeps only the explicitly supplied safe summary`() {
        Logger.w("test", "request body contains secret", IllegalStateException("secret"),
            releaseMessage = "Request failed")

        verify(exactly = 1) { Log.w("test", "Request failed", null) }
    }

    @Test
    fun `debuggable host logs full messages and exceptions for both log settings`() {
        val details = "token=secret email=customer@example.com https://example.com/?private=value"
        val error = IllegalStateException(details, IllegalArgumentException(details))

        for (logLevel in LogLevel.entries) {
            initLogger(debuggable = true, logLevel = logLevel)
            val tag = logLevel.name

            Logger.d(tag, details, releaseMessage = "Safe debug summary")
            Logger.w(tag, details, error, releaseMessage = "Safe warning summary")
            Logger.i(tag, details, error, releaseMessage = "Safe info summary")
            Logger.e(tag, details, error, releaseMessage = "Safe error summary")
            Logger.e(tag, error)

            verify(exactly = 1) { Log.d(tag, details) }
            verify(exactly = 1) { Log.w(tag, details, error) }
            verify(exactly = 1) { Log.i(tag, details, error) }
            verify(exactly = 2) { Log.e(tag, details, error) }
        }
    }

    @Test
    fun `debuggable host logs request and response bodies instead of safe summaries`() {
        initLogger(debuggable = true, logLevel = LogLevel.DEBUG)
        val request = "REQUEST - PUT: https://example.com/device\nRequestBody: {\"alias\":\"test-alias\"}"
        val response = "RESPONSE - PUT: https://example.com/device\nResponseBody: {\"dmcUserId\":\"test-id\"}"

        Logger.w("http", request, releaseMessage = "REQUEST - PUT")
        Logger.i("http", response, releaseMessage = "RESPONSE - PUT: HTTP 200")

        verify(exactly = 1) { Log.w("http", request, null) }
        verify(exactly = 1) { Log.i("http", response, null) }
        verify(exactly = 0) { Log.w("http", "REQUEST - PUT", any()) }
        verify(exactly = 0) { Log.i("http", "RESPONSE - PUT: HTTP 200", any()) }
    }

    @Test
    fun `debug setting disables all SDK logging in a non debuggable host`() {
        initLogger(debuggable = false, logLevel = LogLevel.DEBUG)
        val error = IllegalStateException("secret")

        Logger.d("test", "secret")
        Logger.w("test", "secret", error)
        Logger.i("test", "secret", error)
        Logger.e("test", "secret", error)
        Logger.e("test", error)

        verify(exactly = 0) { Log.d(any(), any()) }
        verify(exactly = 0) { Log.w(any(), any(), any()) }
        verify(exactly = 0) { Log.i(any(), any(), any()) }
        verify(exactly = 0) { Log.e(any(), any(), any()) }
    }

    private fun initLogger(debuggable: Boolean, logLevel: LogLevel) {
        val application = mockk<Application>()
        every { application.applicationInfo } returns mockk<ApplicationInfo>().apply {
            flags = ApplicationInfo.FLAG_HAS_CODE or
                (if (debuggable) ApplicationInfo.FLAG_DEBUGGABLE else 0)
        }
        Logger.init(application, logLevel)
    }
}
