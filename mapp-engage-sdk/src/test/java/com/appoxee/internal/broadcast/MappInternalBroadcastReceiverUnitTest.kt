package com.appoxee.internal.broadcast

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.ui.push.model.PushData
import com.appoxee.internal.util.CompatExt.getParcelableCompat
import com.appoxee.shared.LocalPushBroadcast
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class MappInternalBroadcastReceiverUnitTest {

    private lateinit var sut: MappInternalBroadcastReceiver
    private lateinit var mockContext: Context
    private lateinit var appoxeeContainer: AppoxeeContainer

    @Before
    fun setUp() {
        sut = spyk(MappInternalBroadcastReceiver())
        mockContext = mockk<Context>(relaxed = true)
        appoxeeContainer = mockk<AppoxeeContainer>(relaxed = true)
        sut.setAppoxeeContainer(appoxeeContainer)

        mockkStatic(Log::class)
        every { sut.onReceive(mockContext, any()) } answers { callOriginal() }
        coEvery { sut.notifyClientApp(any(), any(), any()) } just runs
        coEvery { sut.sendReportEvent(any(), any(), any()) } just runs
        every { sut.dismissNotification(any(), any()) } just runs
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onReceive executes successfully with clickType LaunchApp and eventType Click`() = runTest {
        val mockIntent = mockk<Intent>(relaxed = true)
        val mockExtras = mockk<Bundle>(relaxed = true)
        val mockPushData = mockk<PushData>(relaxed = true)

        every { mockExtras.getInt("eventType") } returns EventType.CLICK.ordinal
        every { mockExtras.getInt("notificationId") } returns 1
        every { mockExtras.getParcelableCompat<PushData>("pushData") } returns mockPushData

        every { mockIntent.extras } answers { mockExtras }
        every { mockIntent.action } answers { LocalPushBroadcast.PUSH_OPENED }
        every { mockIntent.getStringExtra("clickType") } answers { ClickType.LAUNCH_APP.value }

        sut.onReceive(context = mockContext, i = mockIntent)

        coVerifyOrder {
            sut.setAppoxeeContainer(any())
            sut.onReceive(any(), any())
            sut.sendReportEvent(mockPushData, ClickType.LAUNCH_APP, EventType.CLICK)
            sut.notifyClientApp(mockContext, mockPushData, LocalPushBroadcast.PUSH_OPENED)
        }
    }

    @Test
    fun `onReceive executes successfully with clickType DISMISS and eventType DISMISS`() = runTest {
        val mockIntent = mockk<Intent>(relaxed = true)
        val mockExtras = mockk<Bundle>(relaxed = true)
        val mockPushData = mockk<PushData>(relaxed = true)

        every { mockExtras.getInt("eventType") } returns EventType.DISMISS.ordinal
        every { mockExtras.getInt("notificationId") } returns 1
        every { mockExtras.getParcelableCompat<PushData>("pushData") } returns mockPushData

        every { mockIntent.extras } answers { mockExtras }
        every { mockIntent.action } answers { LocalPushBroadcast.PUSH_DISMISSED }
        every { mockIntent.getStringExtra("clickType") } answers { ClickType.DISMISS.value }

        sut.onReceive(context = mockContext, i = mockIntent)

        coVerifyOrder {
            sut.sendReportEvent(mockPushData, ClickType.DISMISS, EventType.DISMISS)
            sut.notifyClientApp(mockContext, mockPushData, LocalPushBroadcast.PUSH_DISMISSED)
            sut.dismissNotification(any(), any())
        }
    }


    @Test
    fun `onReceive doesn't send report for PUSH_RECEIVED`() = runTest {
        val mockIntent = mockk<Intent>(relaxed = true)
        val mockExtras = mockk<Bundle>(relaxed = true)
        val mockPushData = mockk<PushData>(relaxed = true)

        every { mockExtras.getInt("eventType") } returns EventType.CLICK.ordinal
        every { mockExtras.getInt("notificationId") } returns 1
        every { mockExtras.getParcelableCompat<PushData>("pushData") } returns mockPushData

        every { mockIntent.extras } answers { mockExtras }
        every { mockIntent.action } answers { LocalPushBroadcast.PUSH_RECEIVED }
        every { mockIntent.getStringExtra("clickType") } answers { ClickType.OPEN_STORE.value }

        sut.onReceive(context = mockContext, i = mockIntent)

        coVerify(exactly = 0){
            sut.sendReportEvent(mockPushData, ClickType.OPEN_STORE, EventType.CLICK)
        }

        coVerify(exactly = 1) {
            sut.notifyClientApp(mockContext, mockPushData, LocalPushBroadcast.PUSH_RECEIVED)
        }
    }
}