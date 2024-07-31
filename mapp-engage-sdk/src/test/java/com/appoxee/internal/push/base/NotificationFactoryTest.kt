package com.appoxee.internal.push.base

import androidx.core.app.NotificationCompat
import com.appoxee.internal.provider.IconProvider
import com.appoxee.internal.provider.PendingIntentProvider
import com.appoxee.internal.push.model.CategoriesFactory
import com.appoxee.internal.push.model.PushData
import com.appoxee.internal.push.style.NotificationStyleFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class NotificationFactoryTest {

    private lateinit var categoriesFactory: CategoriesFactory
    private lateinit var notificationStyleFactory: NotificationStyleFactory
    private lateinit var notificationBuilderFactory: NotificationBuilder
    private lateinit var iconProvider: IconProvider
    private lateinit var pendingIntentProvider: PendingIntentProvider

    private lateinit var notificationFactory: NotificationFactory

    private lateinit var pushData: PushData

    @Before
    fun setUp() {
        pushData = mockk<PushData>(relaxed = true) {}
            .copy(title = "Test Title", bigText = "Test Big Text")

        categoriesFactory = mockk(relaxed = true)

        iconProvider = mockk(relaxed = true) {
            every { getLargeIcon() } returns mockk(relaxed = true)
            every { getSmallIcon() } returns 0
            every { getSmallIconApi23() } returns mockk(relaxed = true)
        }

        pendingIntentProvider = mockk(relaxed = true) {
            every { createPendingIntent(any()) } returns mockk()
            every { createDismissPendingIntent(any(), any()) } returns mockk()
            every { createCustomPendingIntent(any(), any(), any(), any(), any()) } returns mockk()
        }

        notificationStyleFactory = mockk(relaxed = true) {
            every { buildNotificationStyle(any()) } returns mockk(relaxed = true) {
                coEvery { getStyle() } coAnswers { mockk(relaxed = true) }
            }
        }

        notificationBuilderFactory = mockk(relaxed = true) {
            every { setPriority(NotificationCompat.PRIORITY_HIGH) } returns this
            every { setContentTitle(pushData.title) } returns this
            every { setContentText(pushData.bigText) } returns this
            every { setLargeIcon(iconProvider.getLargeIcon()) } returns this
            every { setAutoCancel(true) } returns this
            every { setStyle(any()) } returns this
            every { setSmallIcon(iconProvider.getSmallIcon()) } returns this
            every { setSmallIcon(iconProvider.getSmallIconApi23()) } returns this
            every { setDeleteIntent(any()) } returns this
            every { setContentIntent(any()) } returns this
            every { build() } returns mockk(relaxed = true)
        }

        notificationFactory = NotificationFactory(
            categoriesFactory,
            notificationStyleFactory,
            notificationBuilderFactory,
            iconProvider,
            pendingIntentProvider
        )
    }

    @Test
    fun testCreateSimpleNotification() = runTest {

        val notificationId = 123
        val builder = notificationBuilderFactory

        val notification = notificationFactory.createSimpleNotification(pushData, notificationId)

        verify { builder.setPriority(any()) }
        verify { builder.setContentTitle(any()) }
        verify { builder.setContentText(any()) }
        verify { builder.setLargeIcon(any()) }
        verify { builder.setAutoCancel(true) }
        verify { builder.setStyle(any()) }
        verify { builder.build() }

        assertNotNull(notification)
    }
}
