package com.appoxee.internal.broadcast

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.appoxee.internal.TestDispatchers
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.stats.StatsClient
import com.appoxee.internal.ui.push.model.PushData
import com.appoxee.internal.util.CompatExt.getParcelableCompat
import com.appoxee.internal.util.Dispatchers
import com.appoxee.internal.util.Logger
import com.appoxee.shared.LocalPushBroadcast
import com.google.common.truth.Truth
import io.mockk.Called
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
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
    private lateinit var appoxeeContainer: AppoxeeContainer
    private lateinit var statsClient: StatsClient
    private lateinit var pushData: PushData
    private lateinit var bundle: Bundle
    private lateinit var dispatchers: Dispatchers

    @Before
    fun setUp() {
        dispatchers = TestDispatchers()
        mockkStatic(Logger::class)
        mockkStatic(Log::class)
        context = mockk(relaxed = true)
        intent = mockk(relaxed = true)
        notificationManager = mockk(relaxed = true)
        pushData = mockk(relaxed = true)
        bundle = mockk(relaxed = true)

        appoxeeContainer = spyk(AppoxeeContainer.getInstance(context, dispatchers))

        receiver = spyk(MappInternalBroadcastReceiver(), recordPrivateCalls = true) {
            setAppoxeeContainer(appoxeeContainer)
        }

        statsClient = mockkClass(StatsClient::class, relaxed = true, relaxUnitFun = true)

        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
        every { intent.extras } returns bundle
        every { bundle.getInt("notificationId") } returns 123
        every { bundle.getParcelableCompat<PushData>("pushData") } returns pushData
        every { pushData.id } returns 1L
        every { pushData.sendoutId } returns 2L
        every { appoxeeContainer.statsClient } returns statsClient
        coEvery { appoxeeContainer.storage.getBroadcastClass() } coAnswers { MappInternalBroadcastReceiver::class.java }
    }

    @After
    fun tearDown() {
        unmockkAll()
        unmockkStatic(Logger::class)
        unmockkStatic(Log::class)
    }

    @Test
    fun should_Handle_DISMISS_action_and_send_report_event() = runBlocking {
        val action = LocalPushBroadcast.Action.PUSH_DISMISSED
        every { intent.action } returns action
        every { intent.getIntExtra("notificationId", 123) } returns 123

        coEvery {
            appoxeeContainer.statsClient.reportPushEvent(
                any(),
                any(),
                any(),
                any()
            )
        } just Runs
        every { notificationManager.cancel(any()) } just Runs
        every { pushData.id } returns 1
        every { pushData.sendoutId } returns 2

        receiver.onReceive(context, intent)

        Truth.assertThat(appoxeeContainer.statsClient).isNotNull()
        coVerify {
            appoxeeContainer.statsClient.reportPushEvent(
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun should_not_handle_null_action() = runBlocking {
        every { intent.action } returns null

        receiver.onReceive(context, intent)

        verify { notificationManager wasNot Called }
    }
}
