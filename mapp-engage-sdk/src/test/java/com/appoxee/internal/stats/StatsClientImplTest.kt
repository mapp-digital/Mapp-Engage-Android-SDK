package com.appoxee.internal.stats

import TestDispatchers
import android.util.Log
import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.util.Dispatchers
import com.appoxee.internal.util.Logger
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class StatsClientImplTest {

    private lateinit var statsClient: StatsClientImpl
    private lateinit var engageApi: EngageApi
    private lateinit var dispatchers: Dispatchers

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } answers { 0 }
        every { Log.e(any(), any(), any()) } answers { 0 }

        engageApi = mockk(relaxed = true)
        dispatchers = TestDispatchers()
        statsClient = StatsClientImpl(engageApi, dispatchers)
    }

    @After
    fun tearDown() {
        unmockkStatic(Logger::class)
        unmockkAll()
    }

    @Test
    fun reportPushEvent() = runTest {
        statsClient.reportPushEvent(
            messageId = 1,
            sendoutId = 2,
            clickType = ClickType.OPEN_STORE,
            eventType = EventType.CLICK
        )

        coVerify { engageApi.pushEvent(1, 2, ClickType.OPEN_STORE, EventType.CLICK) }
    }

    @Test
    fun reportInappEvent() = runTest {
        statsClient.reportInappEvent(
            originalEventId = "1",
            templateId = 1,
            trackingKey = TrackingKey.IA_MSG_DISPLAYED,
            trackingAttributes = emptyMap<String, Any>()
        )

        coVerify {
            engageApi.inappEvent(any(), any(), any(), any())
        }
    }

    @Test
    fun reportActivation() = runTest {
        statsClient.reportActivation(seconds = 10)
        coVerify {
            engageApi.activate(10)
        }
    }
}