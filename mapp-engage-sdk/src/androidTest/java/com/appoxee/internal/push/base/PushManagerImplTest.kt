package com.appoxee.internal.push.base

import android.app.Notification
import com.appoxee.internal.push.model.PushData
import com.appoxee.internal.storage.InMemoryStorageImpl
import com.appoxee.internal.storage.Storage
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.NotificationMode
import com.google.common.truth.Truth
import com.google.firebase.messaging.RemoteMessage
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class PushManagerImplTest {
    private lateinit var pushManager: PushManagerImpl
    private lateinit var notificationFactory: NotificationFactory
    private lateinit var storage: Storage
    private lateinit var scope: CoroutineScope
    private lateinit var notify: Notify

    private val CHANNEL_ID = "MAPP_NOTIFICATION_1"
    private val CHANNEL_NAME = "MAPP_NOTIFICATION_CHANNEL"

    @Before
    fun setUp() {
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
        scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

        pushManager = spyk(
            PushManagerImpl(
                scope,
                notify,
                notificationFactory,
                storage,
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
    fun handlePushMessage() {
        runBlocking {
            // setup test conditions
            val remoteMessage = mockk<RemoteMessage>(relaxed = true)
            val appoxeeOptions = mockk<AppoxeeOptions>(relaxed = true)
            every { appoxeeOptions.notificationMode } answers { NotificationMode.BACKGROUND_AND_FOREGROUND }
            coEvery { pushManager.isPushMessageFromMapp(remoteMessage) } coAnswers { true }
            coEvery { storage.getInitOptions() } coAnswers { appoxeeOptions }
            coEvery { pushManager.invokeNoArgs("getNotificationMode") } coAnswers { appoxeeOptions.notificationMode }
            coEvery {
                pushManager.createNotification(
                    any(),
                    any()
                )
            } coAnswers { mockk(relaxUnitFun = true, relaxed = true) }

            // execute method to be tested
            pushManager.handlePushMessage(remoteMessage)

            // validates test results
            verify { pushManager.isPushMessageFromMapp(any()) }
            coVerify { pushManager.createNotification(any(), any()) }
            verify { pushManager.showNotification(any(), any()) }
        }
    }

    @Test
    fun isPushMessageFromMapp() {
        runBlocking {
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
    }

    @Test
    fun createNotification() {
        runBlocking {
            val pushData = mockk<PushData>()
            val notification = pushManager.createNotification(pushData, 1)
            coVerify { notificationFactory.createSimpleNotification(pushData,1) }
            Truth.assertThat(notification).isNotNull()
        }
    }

    @Test
    fun createNotificationChannel() {
        runBlocking {
            pushManager.createNotificationChannel()
            verify(exactly = 1) { notify.createChannel(any(), any(), any()) }
        }
    }

    @Test
    fun showNotification() {
        runBlocking {
            val notification = mockk<Notification>()

            pushManager.showNotification(notification, 1)
            verify(exactly = 1) { notify.showNotification(notification, 1) }
        }
    }

    @Test
    fun dismissNotification() {
        runBlocking {
            pushManager.dismissNotification(1)
            verify { notify.closeNotification(1) }
        }
    }
}