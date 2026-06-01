package com.appoxee.internal.ui.inapp

import android.app.Activity
import android.util.Log
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.model.response.inapp.NativeInappMessage
import com.appoxee.internal.model.response.inapp.TrackingParams
import com.appoxee.internal.model.response.inapp.WebInappMessage
import com.appoxee.internal.stats.StatsClient
import com.appoxee.internal.ui.inapp.nativ.NativeFactory
import com.appoxee.internal.ui.inapp.web.WebFactory
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class InAppManagerImplTest {

    private lateinit var nativeFactory: NativeFactory
    private lateinit var webFactory: WebFactory
    private lateinit var statsClient: StatsClient
    private lateinit var testScope: TestScope
    private lateinit var activity: Activity
    private lateinit var sut: InAppManagerImpl

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        nativeFactory = mockk(relaxed = true)
        webFactory = mockk(relaxed = true)
        statsClient = mockk(relaxed = true)
        activity = mockk(relaxed = true)
        testScope = TestScope(StandardTestDispatcher())

        sut = InAppManagerImpl(
            nativeFactory = nativeFactory,
            webFactory = webFactory,
            statsClient = statsClient,
            scope = testScope,
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun nativeMsg(templateId: Long = 1L, eventId: String = "evt") =
        mockk<NativeInappMessage>(relaxed = true) {
            every { this@mockk.templateId } returns templateId
            every { originalEventId } returns eventId
        }

    private fun webMsg(templateId: Long = 2L, eventId: String = "evt") =
        mockk<WebInappMessage>(relaxed = true) {
            every { this@mockk.templateId } returns templateId
            every { originalEventId } returns eventId
        }

    @Test
    fun `handleMessages with empty list fires no events and shows nothing`() = runTest {
        sut.handleMessages(activity, emptyList())
        testScope.advanceUntilIdle()

        coVerify(exactly = 0) { statsClient.reportInappEvent(any(), any(), any(), any()) }
        coVerify(exactly = 0) { nativeFactory.createBanner(any(), any(), any(), any()) }
        coVerify(exactly = 0) { webFactory.createBanner(any(), any(), any(), any()) }
    }

    @Test
    fun `handleMessages with single message shows it and fires no ia_message_not_displayed`() =
        runTest {
            val msg = nativeMsg()
            every { nativeFactory.createBanner(any(), any(), any(), any()) } just runs

            sut.handleMessages(activity, listOf(msg))
            testScope.advanceUntilIdle()

            coVerify(exactly = 0) {
                statsClient.reportInappEvent(any(), any(), TrackingKey.IA_MSG_NOT_DISPLAYED, any())
            }
        }

    @Test
    fun `handleMessages shows only the newest message (highest templateId)`() = runTest {
        val older = nativeMsg(templateId = 1L)
        val newer = webMsg(templateId = 2L)

        sut.handleMessages(activity, listOf(older, newer))
        testScope.advanceUntilIdle()

        coVerify(exactly = 0) { nativeFactory.createBanner(any(), older, any(), any()) }
        coVerify(exactly = 1) { webFactory.createBanner(any(), newer, any(), any()) }
    }

    @Test
    fun `handleMessages fires ia_message_not_displayed for every skipped message immediately`() =
        runTest {
            val oldest = nativeMsg(templateId = 1L, eventId = "evt1")
            val middle = webMsg(templateId = 2L, eventId = "evt2")
            val newest = webMsg(templateId = 3L, eventId = "evt3")

            // newest (templateId=3) will be shown; oldest and middle discarded
            sut.handleMessages(activity, listOf(oldest, middle, newest))
            testScope.advanceUntilIdle()

            coVerify(exactly = 1) {
                statsClient.reportInappEvent(
                    "evt1", 1L, TrackingKey.IA_MSG_NOT_DISPLAYED,
                    match { it["reason"] == TrackingParams.REASON_OTHER_MESSAGE_DISPLAYING }
                )
            }
            coVerify(exactly = 1) {
                statsClient.reportInappEvent(
                    "evt2", 2L, TrackingKey.IA_MSG_NOT_DISPLAYED,
                    match { it["reason"] == TrackingParams.REASON_OTHER_MESSAGE_DISPLAYING }
                )
            }
        }

    @Test
    fun `onShow callback fires ia_message_displayed for the newest shown message`() = runTest {
        val older = nativeMsg(templateId = 1L, eventId = "evt1")
        val newer = nativeMsg(templateId = 2L, eventId = "evt2")
        val onShowSlot = slot<((NativeInappMessage) -> Unit)>()

        every {
            nativeFactory.createBanner(any(), any(), capture(onShowSlot), any())
        } answers { onShowSlot.captured.invoke(newer) }

        sut.handleMessages(activity, listOf(older, newer))
        testScope.advanceUntilIdle()

        coVerify(exactly = 1) {
            statsClient.reportInappEvent("evt2", 2L, TrackingKey.IA_MSG_DISPLAYED, any())
        }
        coVerify(exactly = 0) {
            statsClient.reportInappEvent("evt1", 1L, TrackingKey.IA_MSG_DISPLAYED, any())
        }
    }

    @Test
    fun `onMessageClosed callback forwards the tracking key to statsClient`() = runTest {
        val msg = nativeMsg(templateId = 1L, eventId = "evt1")
        val onClosedSlot =
            slot<((NativeInappMessage, TrackingKey, TrackingParams) -> Unit)>()

        every {
            nativeFactory.createBanner(any(), any(), any(), capture(onClosedSlot))
        } answers {
            onClosedSlot.captured.invoke(msg, TrackingKey.IA_MSG_DISMISSED, TrackingParams(reason = TrackingParams.REASON_USER_DISMISSED))
        }

        sut.handleMessages(activity, listOf(msg))
        testScope.advanceUntilIdle()

        coVerify(exactly = 1) {
            statsClient.reportInappEvent(
                "evt1", 1L, TrackingKey.IA_MSG_DISMISSED,
                match { it["reason"] == TrackingParams.REASON_USER_DISMISSED }
            )
        }
    }

    @Test
    fun `handleMessages with 3 native messages fires not_displayed for older 2 only`() =
        runTest {
            val oldest = nativeMsg(templateId = 1L, eventId = "e1")
            val middle = nativeMsg(templateId = 2L, eventId = "e2")
            val newest = nativeMsg(templateId = 3L, eventId = "e3")

            // newest (templateId=3) shown; oldest and middle discarded
            sut.handleMessages(activity, listOf(oldest, middle, newest))
            testScope.advanceUntilIdle()

            coVerify(exactly = 1) {
                statsClient.reportInappEvent("e1", 1L, TrackingKey.IA_MSG_NOT_DISPLAYED, any())
            }
            coVerify(exactly = 1) {
                statsClient.reportInappEvent("e2", 2L, TrackingKey.IA_MSG_NOT_DISPLAYED, any())
            }
            coVerify(exactly = 0) {
                statsClient.reportInappEvent("e3", 3L, TrackingKey.IA_MSG_NOT_DISPLAYED, any())
            }
        }
}
