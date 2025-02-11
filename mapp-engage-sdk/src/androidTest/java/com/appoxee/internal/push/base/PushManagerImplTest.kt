package com.appoxee.internal.push.base

import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.appoxee.internal.TestDispatchers
import com.appoxee.internal.storage.InMemoryStorageImpl
import com.appoxee.internal.storage.Storage
import com.appoxee.internal.ui.push.base.NotificationFactory
import com.appoxee.internal.ui.push.base.Notify
import com.appoxee.internal.ui.push.base.PushManagerImpl
import com.appoxee.internal.ui.push.model.CategoriesFactory
import com.appoxee.internal.ui.push.model.PushData
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.NotificationMode
import com.google.common.truth.Truth
import com.google.firebase.messaging.RemoteMessage
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class PushManagerImplTest {
    private lateinit var pushManager: PushManagerImpl
    private lateinit var notificationFactory: NotificationFactory
    private lateinit var storage: Storage
    private lateinit var notify: Notify
    private lateinit var dispatchers: com.appoxee.internal.util.Dispatchers
    private lateinit var categoriesFactory: CategoriesFactory
    private lateinit var context: Context

    private val CHANNEL_ID = "MAPP_NOTIFICATION_1"
    private val CHANNEL_NAME = "MAPP_NOTIFICATION_CHANNEL"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dispatchers = TestDispatchers()
        notify = mockk(relaxed = true, relaxUnitFun = true)
        notificationFactory = mockk<NotificationFactory>(relaxed = true) {
            coEvery { createSimpleNotification(any(), any()) } coAnswers {
                mockk(
                    relaxed = true,
                    relaxUnitFun = true
                )
            }
        }
        storage = spyk(InMemoryStorageImpl())
        categoriesFactory = spyk(CategoriesFactory(storage))

        pushManager = spyk(
            PushManagerImpl(
                dispatchers,
                notify,
                notificationFactory,
                storage,
                categoriesFactory,
                CHANNEL_ID,
                CHANNEL_NAME
            )
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun handlePushMessage() = runTest {
        // setup test conditions
        val remoteMessage = mockk<RemoteMessage>(relaxed = true)
        val appoxeeOptions = mockk<AppoxeeOptions>(relaxed = true)
        every { appoxeeOptions.notificationMode } answers { NotificationMode.BACKGROUND_AND_FOREGROUND }
        coEvery { pushManager.isPushMessageFromMapp(remoteMessage) } coAnswers { true }
        coEvery { storage.getInitOptions() } coAnswers { appoxeeOptions }
        coJustRun { pushManager.reportPushReceived(any(), any(), any()) }
        coEvery { pushManager.invokeNoArgs("getNotificationMode") } coAnswers { appoxeeOptions.notificationMode }
        coEvery {
            pushManager.createNotification(
                any(),
                any()
            )
        } coAnswers { mockk(relaxUnitFun = true, relaxed = true) }

        // execute method to be tested
        pushManager.handlePushMessage(context, remoteMessage)

        // validates test results
        verify { pushManager.isPushMessageFromMapp(any()) }
        coVerify { pushManager.createNotification(any(), any()) }
        verify { pushManager.showNotification(any(), any()) }
    }

    @Test
    fun isPushMessageFromMapp() = runTest {
        val bundle = mutableMapOf<String, String>().apply {
            put("p", "1234")
            put("category", "apx_acc_dec_open")
            put("user_id", "61134251597")
            put("customer_id", "60211")
        }
        val remoteMessage = mockk<RemoteMessage>() {
            every { data } answers { bundle }
        }

        // methods checks if push data are from mapp based on condition that parameters
        // "p", "category", "user_id", "customer_id" are NOT NULL
        val result = pushManager.isPushMessageFromMapp(remoteMessage)
        Truth.assertThat(result).isTrue()
    }


    @Test
    fun createNotification() = runTest {
        val pushData = mockk<PushData>()
        val notification = pushManager.createNotification(pushData, 1)
        coVerify { notificationFactory.createSimpleNotification(pushData, 1) }
        Truth.assertThat(notification).isNotNull()
    }

    @Test
    fun createNotificationChannel() = runTest {
        justRun { notify.createChannel(any(), any(), any()) }
        pushManager.createNotificationChannel()
        verify(exactly = 1) { notify.createChannel(any(), any(), any()) }
    }

    @Test
    fun showNotification() = runTest {
        val notification = mockk<Notification>()

        pushManager.showNotification(notification, 1)
        verify(exactly = 1) { notify.showNotification(notification, 1) }
    }


    @Test
    fun dismissNotification() = runTest {
        pushManager.dismissNotification(1)
        verify { notify.closeNotification(1) }
    }
}