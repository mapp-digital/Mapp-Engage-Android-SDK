package com.appoxee.internal.ui.push.base

import TestDispatchersProvider
import android.app.Notification
import android.content.Context
import android.util.Log
import com.appoxee.internal.AppoxeeAdapter
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.model.response.Category
import com.appoxee.internal.storage.Storage
import com.appoxee.internal.ui.ActivityLifecycleHandler
import com.appoxee.internal.ui.push.model.CategoriesFactory
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.LocalPushBroadcast
import com.appoxee.shared.NotificationMode
import com.google.common.truth.Truth.assertThat
import com.google.firebase.messaging.RemoteMessage
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PushManagerImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchersProvider = TestDispatchersProvider(testDispatcher)

    private lateinit var sut: PushManagerImpl

    private lateinit var notify: Notify
    private lateinit var notificationFactory: NotificationFactory
    private lateinit var appoxeeContainer: AppoxeeContainer
    private lateinit var categoriesFactory: CategoriesFactory
    private lateinit var storage: Storage
    private lateinit var activityLifecycleHandler: ActivityLifecycleHandler
    private lateinit var context: Context

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } answers { 0 }
        every { Log.i(any(), any()) } answers { 0 }
        every { Log.i(any(), any(), any()) } answers { 0 }
        every { Log.e(any(), any(), any()) } answers { 0 }

        notify = mockk(relaxed = true)
        notificationFactory = mockk(relaxed = true)
        appoxeeContainer = mockk(relaxed = true)
        categoriesFactory = mockk(relaxed = true)
        storage = mockk(relaxed = true)
        activityLifecycleHandler = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { appoxeeContainer.storage } returns storage
        every { appoxeeContainer.activityLifecycleHandler } returns activityLifecycleHandler
        every { appoxeeContainer.appoxeeAdapter } returns mockk<AppoxeeAdapter>(relaxed = true)
        coEvery { categoriesFactory.getCategories() } returns emptyList<Category>()
        every { notificationFactory.createSimpleNotification(any(), any()) } returns mockk<Notification>(
            relaxed = true
        )

        sut = spyk(
            PushManagerImpl(
                dispatchersProvider = dispatchersProvider,
                notify = notify,
                notificationFactory = notificationFactory,
                appoxeeContainer = appoxeeContainer,
                categoriesFactory = categoriesFactory,
                notificationChannelId = "channel-id",
                notificationChannelName = "channel-name"
            )
        )
        every { sut.reportPushReceived(any(), any(), any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `silent only never shows notification UI but still broadcasts regular push event`() = runTest(testDispatcher) {
        coEvery { storage.getInitOptions() } returns options(NotificationMode.SILENT_ONLY)
        every { activityLifecycleHandler.isInForeground() } returns false

        sut.handlePushMessage(context, regularRemoteMessage())

        verify(exactly = 0) { notify.showNotification(any(), any()) }
        verify(exactly = 1) { sut.reportPushReceived(context, any(), LocalPushBroadcast.PUSH_RECEIVED) }
    }

    @Test
    fun `background only shows notification when app is not in foreground and broadcasts event`() = runTest(testDispatcher) {
        coEvery { storage.getInitOptions() } returns options(NotificationMode.BACKGROUND_ONLY)
        every { activityLifecycleHandler.isInForeground() } returns false

        sut.handlePushMessage(context, regularRemoteMessage())

        verify(exactly = 1) { notify.showNotification(any(), any()) }
        verify(exactly = 1) { sut.reportPushReceived(context, any(), LocalPushBroadcast.PUSH_RECEIVED) }
    }

    @Test
    fun `background only hides notification when app is in foreground and still broadcasts event`() = runTest(testDispatcher) {
        coEvery { storage.getInitOptions() } returns options(NotificationMode.BACKGROUND_ONLY)
        every { activityLifecycleHandler.isInForeground() } returns true

        sut.handlePushMessage(context, regularRemoteMessage())

        verify(exactly = 0) { notify.showNotification(any(), any()) }
        verify(exactly = 1) { sut.reportPushReceived(context, any(), LocalPushBroadcast.PUSH_RECEIVED) }
    }

    @Test
    fun `background and foreground always shows notification and broadcasts event`() = runTest(testDispatcher) {
        coEvery { storage.getInitOptions() } returns options(NotificationMode.BACKGROUND_AND_FOREGROUND)
        every { activityLifecycleHandler.isInForeground() } returns true

        sut.handlePushMessage(context, regularRemoteMessage())

        verify(exactly = 1) { notify.showNotification(any(), any()) }
        verify(exactly = 1) { sut.reportPushReceived(context, any(), LocalPushBroadcast.PUSH_RECEIVED) }
    }

    @Test
    fun `notification mode enum keeps expected order`() {
        assertThat(NotificationMode.entries).containsExactly(
            NotificationMode.BACKGROUND_ONLY,
            NotificationMode.SILENT_ONLY,
            NotificationMode.BACKGROUND_AND_FOREGROUND
        ).inOrder()
    }

    private fun options(notificationMode: NotificationMode) = AppoxeeOptions(
        server = AppoxeeOptions.Server.TEST,
        sdkKey = "1111.2222",
        appId = "33333",
        tenantId = "44444"
    ).also {
        it.notificationMode = notificationMode
    }

    private fun regularRemoteMessage(): RemoteMessage = mockk {
        every { data } returns mapOf(
            "p" to "123",
            "push_title" to "title",
            "alert" to "message"
        )
        every { priority } returns RemoteMessage.PRIORITY_HIGH
    }
}
