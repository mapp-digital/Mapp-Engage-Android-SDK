package com.appoxee.internal.broadcast

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.appoxee.internal.container.StatsContainer
import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.push.model.PushData
import com.appoxee.internal.stats.StatsClient
import com.appoxee.internal.util.CompatExt.getParcelableCompat
import com.appoxee.internal.util.Logger
import com.google.common.truth.Truth
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class MappInternalBroadcastReceiverTest {
    private lateinit var receiver: MappInternalBroadcastReceiver
    private lateinit var context: Context
    private lateinit var intent: Intent
    private lateinit var notificationManager: NotificationManager
    private lateinit var statsContainer: StatsContainer
    private lateinit var statsClient: StatsClient
    private lateinit var pushData: PushData
    private lateinit var bundle: Bundle

    @Before
    fun setUp() {
        mockkStatic(Logger::class)
        mockkStatic(Log::class)
        context = mockk(relaxed = true)
        intent = mockk(relaxed = true)
        notificationManager = mockk(relaxed = true)
        pushData = mockk(relaxed = true)
        bundle = mockk(relaxed = true)

        statsContainer = spyk(StatsContainer(context), recordPrivateCalls = true)

        receiver = spyk(MappInternalBroadcastReceiver(), recordPrivateCalls = true) {
            setStatsContainer(statsContainer)
        }

        statsClient = mockkClass(StatsClient::class, relaxed = true, relaxUnitFun = true)

        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
        every { intent.extras } returns bundle
        every { bundle.getInt("notificationId") } returns 123
        every { bundle.getParcelableCompat<PushData>("pushData") } returns pushData
        every { pushData.id } returns 1L
        every { pushData.sendoutId } returns 2L
        every { statsContainer.statsClient } returns statsClient
    }

    @After
    fun tearDown() {
        unmockkAll()
        unmockkStatic(Logger::class)
        unmockkStatic(Log::class)
    }

    @Test
    fun should_Handle_DISMISS_action_and_send_report_event() = runBlocking {
        val clickType = ClickType.DISMISS
        every { intent.action } returns clickType.value
        every { statsContainer.statsClient.reportPushEvent(any(), any(), any(), any()) } just Runs
        every { notificationManager.cancel(any()) } just Runs
        every { pushData.id } returns 1
        every { pushData.sendoutId } returns 2

        receiver.onReceive(context, intent)

        verify { notificationManager.cancel(123) }
        verify { statsContainer.statsClient }
        Truth.assertThat(statsContainer.statsClient).isNotNull()
        verify {
            statsContainer.statsClient.reportPushEvent(
                1,
                2,
                ClickType.DISMISS,
                EventType.DISMISS
            )
        }
    }

    @Test
    fun should_not_handle_null_action() = runBlocking {
        every { intent.action } returns null

        receiver.onReceive(context, intent)

        verify { notificationManager wasNot Called }
    }

    @Test
    fun should_not_handle_non_DISMISS_action() = runBlocking {
        val clickType = ClickType.OPEN_STORE
        every { intent.action } returns clickType.value

        receiver.onReceive(context, intent)

        verify { notificationManager wasNot Called }
        verify { statsContainer.statsClient wasNot Called }
    }

}
