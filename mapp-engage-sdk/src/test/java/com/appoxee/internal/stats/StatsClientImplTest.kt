package com.appoxee.internal.stats

import TestDispatchersProvider
import android.util.Log
import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.ResponseData
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.network.response.Response
import com.appoxee.internal.util.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class StatsClientImplTest {

    // system under test
    private lateinit var sut: StatsClientImpl
    private lateinit var engageApi: EngageApi
    private val scheduler = TestCoroutineScheduler()
    private val standardTestDispatcher = StandardTestDispatcher(scheduler)
    private val testDispatchers = TestDispatchersProvider(standardTestDispatcher)
    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        mockkStatic(Log::class)
        kotlinx.coroutines.Dispatchers.setMain(testDispatchers.mainDispatcher)
        every { Log.d(any(), any()) } answers { 0 }
        every { Log.e(any(), any(), any()) } answers { 0 }

        engageApi = mockk(relaxed = true)
        sut = spyk(StatsClientImpl(engageApi, testDispatchers))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        unmockkStatic(Logger::class)
        unmockkAll()
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `report push event executes successfully`() = runTest {
        val mockResponse = Response.success(200, ResponseData(null, true))
        coEvery { engageApi.pushEvent(any(), any(), any(), any()) } coAnswers { mockResponse }
        sut.reportPushEvent(
            messageId = 1,
            sendoutId = 2,
            clickType = ClickType.OPEN_STORE,
            eventType = EventType.CLICK
        )

        coVerify {
            engageApi.pushEvent(1, 2, ClickType.OPEN_STORE, EventType.CLICK)
            Logger.d(any(), any())
        }
    }

    @Test
    fun `report push event fails when engageApi throws exception`() = runTest {
        val mockResponse = Response.error<ResponseData<Boolean>>(Throwable("Error"))
        coEvery { engageApi.pushEvent(any(), any(), any(), any()) } coAnswers { mockResponse }
        sut.reportPushEvent(
            messageId = 1,
            sendoutId = 2,
            clickType = ClickType.OPEN_STORE,
            eventType = EventType.CLICK
        )

        coVerify {
            engageApi.pushEvent(1, 2, ClickType.OPEN_STORE, EventType.CLICK)
            Logger.e(any(), any(), any(Throwable::class))
        }
    }

    @Test
    fun `report inapp event executes successfully`() = runTest {
        val mockResponse = Response.success(200, ResponseData(null, true))
        coEvery { engageApi.inappEvent(any(), any(), any(), any()) } coAnswers { mockResponse }
        sut.reportInappEvent(
            originalEventId = "1",
            templateId = 1,
            trackingKey = TrackingKey.IA_MSG_DISPLAYED,
            trackingAttributes = emptyMap<String, Any>()
        )

        coVerifyOrder {
            engageApi.inappEvent("1", 1, TrackingKey.IA_MSG_DISPLAYED, emptyMap<String, Any>())
            Logger.d(any(), any())
        }
    }

    @Test
    fun `report inapp event fails when engageApi throws exception`() = runTest {
        val mockResponse = Response.error<ResponseData<Boolean>>(Throwable("Error"))
        coEvery { engageApi.inappEvent(any(), any(), any(), any()) } coAnswers { mockResponse }
        sut.reportInappEvent(
            originalEventId = "1",
            templateId = 1,
            trackingKey = TrackingKey.IA_MSG_DISPLAYED,
            trackingAttributes = emptyMap<String, Any>()
        )

        coVerifyOrder {
            engageApi.inappEvent("1", 1, TrackingKey.IA_MSG_DISPLAYED, emptyMap<String, Any>())
            Logger.e(any(), any(),any(Throwable::class))
        }
    }

    @Test
    fun reportActivation() = runTest {
        sut.reportActivation(seconds = 10)
        coVerify {
            engageApi.activate(10)
        }
    }
}