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
        val application = mockk<Application>()
        every { application.applicationInfo } returns mockk<ApplicationInfo>().apply { flags = 0 }
        Logger.init(application, LogLevel.RELEASE)
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
}
