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

    private lateinit var sut: StatsClientImpl
    private lateinit var engageApi: EngageApi
    private val scheduler = TestCoroutineScheduler()
    private val standardTestDispatcher = StandardTestDispatcher(scheduler)
    private val testDispatchers = TestDispatchersProvider(standardTestDispatcher)

    private val success = Response.success(200, ResponseData(null, true))
    private val failure = Response.error<ResponseData<Boolean>>(Throwable("network error"))

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

    // ── push events ──────────────────────────────────────────────────────────

    @Test
    fun `report push event executes successfully`() = runTest {
        coEvery { engageApi.pushEvent(any(), any(), any(), any()) } returns success
        sut.reportPushEvent(1, 2, ClickType.OPEN_STORE, EventType.CLICK)
        coVerify {
            engageApi.pushEvent(1, 2, ClickType.OPEN_STORE, EventType.CLICK)
            Logger.d(any(), any())
        }
    }

    @Test
    fun `report push event fails when engageApi throws exception`() = runTest {
        coEvery { engageApi.pushEvent(any(), any(), any(), any()) } returns failure
        sut.reportPushEvent(1, 2, ClickType.OPEN_STORE, EventType.CLICK)
        coVerify {
            engageApi.pushEvent(1, 2, ClickType.OPEN_STORE, EventType.CLICK)
            Logger.e(any(), any(), any(Throwable::class))
        }
    }

    // ── inapp events — happy path ─────────────────────────────────────────────

    @Test
    fun `report inapp event executes successfully on first attempt`() = runTest {
        coEvery { engageApi.inappEvent(any(), any(), any(), any()) } returns success
        sut.reportInappEvent("1", 1, TrackingKey.IA_MSG_DISPLAYED, emptyMap<String, Any>())
        coVerifyOrder {
            engageApi.inappEvent("1", 1, TrackingKey.IA_MSG_DISPLAYED, emptyMap<String, Any>())
            Logger.d(any(), any())
        }
    }

    // ── inapp events — immediate retry ────────────────────────────────────────

    @Test
    fun `report inapp event succeeds on immediate retry (2nd attempt)`() = runTest {
        coEvery {
            engageApi.inappEvent(any(), any(), any(), any())
        } returnsMany listOf(failure, success)

        sut.reportInappEvent("1", 1, TrackingKey.IA_MSG_DISPLAYED, emptyMap<String, Any>())

        // exactly 2 calls: initial + 1 immediate retry
        coVerify(exactly = 2) {
            engageApi.inappEvent("1", 1, TrackingKey.IA_MSG_DISPLAYED, emptyMap<String, Any>())
        }
    }

    @Test
    fun `report inapp event buffers after both initial attempts fail`() = runTest {
        coEvery { engageApi.inappEvent(any(), any(), any(), any()) } returns failure

        sut.reportInappEvent("1", 1, TrackingKey.IA_MSG_DISPLAYED, emptyMap<String, Any>())

        // exactly 2 calls: initial + immediate retry; no more (buffered, not retried inline)
        coVerify(exactly = 2) {
            engageApi.inappEvent("1", 1, TrackingKey.IA_MSG_DISPLAYED, emptyMap<String, Any>())
        }
    }

    // ── drain cycle ───────────────────────────────────────────────────────────

    @Test
    fun `drain sends buffered event when next event succeeds`() = runTest {
        val sut = StatsClientImpl(engageApi, testDispatchers, maxDrainAttempts = 3)

        // First event — both attempts fail → buffered
        coEvery { engageApi.inappEvent("evt1", any(), any(), any()) } returns failure
        sut.reportInappEvent("evt1", 1, TrackingKey.IA_MSG_NOT_DISPLAYED, emptyMap<String, Any>())

        // Second event — first attempt succeeds → triggers drain of buffered evt1
        coEvery { engageApi.inappEvent("evt2", any(), any(), any()) } returns success
        coEvery { engageApi.inappEvent("evt1", any(), any(), any()) } returns success
        sut.reportInappEvent("evt2", 2, TrackingKey.IA_MSG_DISPLAYED, emptyMap<String, Any>())

        // evt1: 2 (initial pair) + 1 (drain) = 3 total
        coVerify(exactly = 3) { engageApi.inappEvent("evt1", any(), any(), any()) }
        // evt2: 1 (success on first try)
        coVerify(exactly = 1) { engageApi.inappEvent("evt2", any(), any(), any()) }
    }

    @Test
    fun `drain re-buffers event that fails during drain cycle`() = runTest {
        val sut = StatsClientImpl(engageApi, testDispatchers, maxDrainAttempts = 3)

        // First event always fails → will be buffered then re-buffered on each drain
        coEvery { engageApi.inappEvent("evt1", any(), any(), any()) } returns failure
        sut.reportInappEvent("evt1", 1, TrackingKey.IA_MSG_NOT_DISPLAYED, emptyMap<String, Any>())
        // 2 calls so far (initial pair)

        // Second event succeeds → drain runs, evt1 fails drain → re-buffered (drainCount=1)
        coEvery { engageApi.inappEvent("evt2", any(), any(), any()) } returns success
        sut.reportInappEvent("evt2", 2, TrackingKey.IA_MSG_DISPLAYED, emptyMap<String, Any>())
        // evt1 now at drainCount=1, still in buffer

        // Third event succeeds → drain runs again, evt1 fails again → re-buffered (drainCount=2)
        coEvery { engageApi.inappEvent("evt3", any(), any(), any()) } returns success
        sut.reportInappEvent("evt3", 3, TrackingKey.IA_MSG_DISPLAYED, emptyMap<String, Any>())
        // evt1 now at drainCount=2

        // evt1 total so far: 2 (initial) + 1 (drain1) + 1 (drain2) = 4
        coVerify(exactly = 4) { engageApi.inappEvent("evt1", any(), any(), any()) }
    }

    @Test
    fun `drain drops event after maxDrainAttempts exceeded`() = runTest {
        val sut = StatsClientImpl(engageApi, testDispatchers, maxDrainAttempts = 3)

        coEvery { engageApi.inappEvent("evt1", any(), any(), any()) } returns failure
        sut.reportInappEvent("evt1", 1, TrackingKey.IA_MSG_NOT_DISPLAYED, emptyMap<String, Any>())
        // 2 calls (initial pair), buffered with drainCount=0

        coEvery { engageApi.inappEvent("success", any(), any(), any()) } returns success

        // 3 drain cycles — each one increments drainCount and re-buffers on failure
        repeat(3) { i ->
            sut.reportInappEvent("success", i.toLong(), TrackingKey.IA_MSG_DISPLAYED, emptyMap<String, Any>())
        }
        // After 3rd drain: drainCount reaches maxDrainAttempts (3) → dropped on 4th drain trigger
        // So evt1 is attempted in drains 1, 2, 3 → 3 drain calls + 2 initial = 5 total

        // 4th successful event triggers drain but evt1 is now dropped (drainCount > 3)
        sut.reportInappEvent("success", 99, TrackingKey.IA_MSG_DISPLAYED, emptyMap<String, Any>())

        // Total for evt1: 2 initial + 3 drain attempts = 5; no 6th call
        coVerify(exactly = 5) { engageApi.inappEvent("evt1", any(), any(), any()) }
    }

    @Test
    fun `drain processes only events buffered before the cycle starts`() = runTest {
        val sut = StatsClientImpl(engageApi, testDispatchers, maxDrainAttempts = 3)

        // evt1 and evt2 both fail → both buffered
        coEvery { engageApi.inappEvent("evt1", any(), any(), any()) } returns failure
        coEvery { engageApi.inappEvent("evt2", any(), any(), any()) } returns failure
        sut.reportInappEvent("evt1", 1, TrackingKey.IA_MSG_NOT_DISPLAYED, emptyMap<String, Any>())
        sut.reportInappEvent("evt2", 2, TrackingKey.IA_MSG_NOT_DISPLAYED, emptyMap<String, Any>())
        // evt1: 2 calls, evt2: 2 calls

        // evt3 succeeds → drains both evt1 and evt2 (snapshot taken before drain)
        // evt1 fails drain → re-buffered; evt2 fails drain → re-buffered
        coEvery { engageApi.inappEvent("evt3", any(), any(), any()) } returns success
        sut.reportInappEvent("evt3", 3, TrackingKey.IA_MSG_DISPLAYED, emptyMap<String, Any>())

        // evt1: 2 + 1 = 3; evt2: 2 + 1 = 3
        coVerify(exactly = 3) { engageApi.inappEvent("evt1", any(), any(), any()) }
        coVerify(exactly = 3) { engageApi.inappEvent("evt2", any(), any(), any()) }
    }

    // ── buffer cap ───────────────────────────────────────────────────────────

    @Test
    fun `buffer drops oldest event when cap is reached`() = runTest {
        val sut = StatsClientImpl(engageApi, testDispatchers, maxDrainAttempts = 3, maxBufferSize = 2)

        coEvery { engageApi.inappEvent(any(), any(), any(), any()) } returns failure

        // Fill buffer to cap: evt1 and evt2 buffered (each costs 2 calls)
        sut.reportInappEvent("evt1", 1, TrackingKey.IA_MSG_NOT_DISPLAYED, emptyMap<String, Any>())
        sut.reportInappEvent("evt2", 2, TrackingKey.IA_MSG_NOT_DISPLAYED, emptyMap<String, Any>())

        // evt3 overflows the cap — evt1 (oldest) is dropped from buffer, evt3 takes its place
        sut.reportInappEvent("evt3", 3, TrackingKey.IA_MSG_NOT_DISPLAYED, emptyMap<String, Any>())

        // Now trigger a drain via a successful event
        coEvery { engageApi.inappEvent("success", any(), any(), any()) } returns success
        sut.reportInappEvent("success", 99, TrackingKey.IA_MSG_DISPLAYED, emptyMap<String, Any>())

        // evt1 was evicted — only initial 2 calls, never drained
        coVerify(exactly = 2) { engageApi.inappEvent("evt1", any(), any(), any()) }
        // evt2 and evt3 were in buffer and got one drain attempt each
        coVerify(exactly = 3) { engageApi.inappEvent("evt2", any(), any(), any()) }
        coVerify(exactly = 3) { engageApi.inappEvent("evt3", any(), any(), any()) }
    }

    // ── activation ────────────────────────────────────────────────────────────

    @Test
    fun reportActivation() = runTest {
        sut.reportActivation(seconds = 10)
        coVerify { engageApi.activate(10) }
    }
}
