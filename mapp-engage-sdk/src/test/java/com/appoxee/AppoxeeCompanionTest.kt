package com.appoxee

import android.app.Application
import android.content.Context
import android.os.Looper
import android.util.Log
import com.appoxee.shared.AppoxeeOptions
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

class AppoxeeCompanionTest {

    @Before
    fun setUp() {
        mockkStatic(Looper::class)
        mockkStatic(Log::class)

        val mainLooper = mockk<Looper>()
        every { Looper.getMainLooper() } returns mainLooper
        every { mainLooper.thread } answers { Thread.currentThread() }
        every { Log.d(any(), any()) } returns 0
        Appoxee.resetForTests()
        Appoxee.instanceFactory = { _, _, _ -> mockk<Appoxee>(relaxed = true) }
    }

    @After
    fun tearDown() {
        Appoxee.resetForTests()
        unmockkAll()
    }

    @Test
    fun `engage does not log raw options`() {
        val context = mockk<Context>(relaxed = true)
        val application = mockk<Application>(relaxed = true)
        every { context.applicationContext } returns application

        val logMessage = slot<String>()
        every { Log.d(any(), capture(logMessage)) } returns 0

        val options = AppoxeeOptions(
            server = AppoxeeOptions.Server.TEST,
            sdkKey = "sensitive-sdk-key",
            appId = "sensitive-app-id",
            tenantId = "tenant"
        )

        Appoxee.engage(context, options)

        assertThat(logMessage.captured).contains("optionsProvided=true")
        assertThat(logMessage.captured).doesNotContain("sensitive-sdk-key")
        assertThat(logMessage.captured).doesNotContain("sensitive-app-id")
    }

    @Test
    fun `concurrent engage and instance access is stable`() {
        val context = mockk<Context>(relaxed = true)
        val application = mockk<Application>(relaxed = true)
        every { context.applicationContext } returns application

        val errors = CopyOnWriteArrayList<Throwable>()
        val threadCount = 8
        val iterations = 25
        val done = CountDownLatch(threadCount)

        repeat(threadCount) {
            thread(start = true) {
                try {
                    repeat(iterations) {
                        Appoxee.engage(context, null)
                        Appoxee.instance()
                    }
                } catch (t: Throwable) {
                    errors.add(t)
                } finally {
                    done.countDown()
                }
            }
        }

        done.await()

        assertThat(errors).isEmpty()
        assertThat(Appoxee.instance()).isNotNull()
    }

}
