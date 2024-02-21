package com.appoxee.internal.push.base

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
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
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
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
    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notificationFactory: NotificationFactory
    private lateinit var storage: Storage
    private lateinit var scope: CoroutineScope

    private val CHANNEL_ID = "CHANNEL_ID_1"
    private val CHANNEL_NAME = "CHANNEL_NAME"

    @Before
    fun setUp() {
        context = spyk(ApplicationProvider.getApplicationContext())
        notificationManager = spyk(NotificationManagerCompat.from(context))
        notificationFactory = mockk<NotificationFactory>(relaxed = true)
        storage = spyk(InMemoryStorageImpl())
        scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

        pushManager = spyk(
            PushManagerImpl(
                context,
                scope,
                notificationManager,
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
            val remoteMessage = mockk<RemoteMessage>(relaxed = true)
            val appoxeeOptions = mockk<AppoxeeOptions>(relaxed = true)
            every { appoxeeOptions.notificationMode } answers { NotificationMode.BACKGROUND_AND_FOREGROUND }
            coEvery { pushManager.isPushMessageFromMapp(any()) } coAnswers { true }
            coEvery { storage.getInitOptions() } coAnswers { appoxeeOptions }
            coEvery { pushManager.invokeNoArgs("getNotificationMode") } coAnswers { appoxeeOptions.notificationMode }
            coEvery { storage.getInitOptions() } coAnswers { appoxeeOptions }
            pushManager.handlePushMessage(remoteMessage)
            coVerify(exactly = 1) {
                pushManager.showNotification(any(), any(), any())
            }
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
            Truth.assertThat(notification).isNotNull()
        }
    }

    @Test
    fun createNotificationChannel() {
        runBlocking {
            pushManager.createNotificationChannel()
            verify(exactly = 1) { notificationManager.createNotificationChannel(any<NotificationChannel>()) }
        }
    }

    @Test
    fun showNotification() {
        runBlocking {
            val notification = mockk<Notification>()
            every { context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) } answers { PackageManager.PERMISSION_GRANTED }
            every {
                notificationManager.notify(
                    any(Int::class),
                    any(Notification::class)
                )
            } just Runs
            pushManager.showNotification(context, notification, 1)
            verify(exactly = 1) { notificationManager.notify(1, notification) }
        }
    }

    @Test
    fun dismissNotification() {
        runBlocking {
            every { notificationManager.cancel(any(Int::class)) } just Runs
            pushManager.dismissNotification(1)
            verify { notificationManager.cancel(1) }
        }
    }
}