package com.appoxee.internal.push.base

import android.app.Notification
import com.appoxee.internal.provider.IconProvider
import com.appoxee.internal.provider.PendingIntentProvider
import com.appoxee.internal.ui.push.base.NotificationBuilder
import com.appoxee.internal.ui.push.base.NotificationFactory
import com.appoxee.internal.ui.push.model.CategoriesFactory
import com.appoxee.internal.ui.push.model.PushData
import com.appoxee.internal.ui.push.style.NotificationStyle
import com.appoxee.internal.ui.push.style.NotificationStyleFactory
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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
            .copy(title = "Test Title", bigText = "Test Big Text", priority = 1)

        categoriesFactory = mockk<CategoriesFactory>(relaxed = true).also {
            coEvery { it.getCategories() } coAnswers { emptyList() }
        }

        iconProvider = mockk<IconProvider>(relaxed = true).also {
            every { it.getLargeIcon() } returns null
            every { it.getSmallIcon() } returns 0
            every { it.getSmallIconApi23() } returns null
        }

        pendingIntentProvider = mockk<PendingIntentProvider>(relaxed = true)

        every {
            pendingIntentProvider.createPendingIntent(
                any(),
                any(),
                any()
            )
        } returns mockk(relaxed = true)
        every { pendingIntentProvider.createDismissPendingIntent(any(), any()) } returns mockk(
            relaxed = true
        )
        every {
            pendingIntentProvider.createCustomPendingIntent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns mockk(relaxed = true)

        notificationStyleFactory = mockk<NotificationStyleFactory>(relaxed = true).also {
            every { it.buildNotificationStyle(any()) } returns mockk<NotificationStyle>(relaxed = true).also {
                coEvery { it.getStyle() } coAnswers { mockk(relaxed = true) }
            }
        }

        notificationBuilderFactory = mockk<NotificationBuilder>(relaxed = true).also {
            every { it.setPriority(any()) } returns it
            every { it.setAutoCancel(true) } returns it
            every { it.setContentTitle(any()) } returns it
            every { it.setContentText(any()) } returns it
            every { it.setLargeIcon(any()) } returns it
            every { it.setStyle(any()) } returns it
            every { it.setSmallIcon(any<Int>()) } returns it
            every { it.setSmallIcon(null) } returns it
            every { it.setDeleteIntent(any()) } returns it
            every { it.setContentIntent(any()) } returns it
            every { it.build() } returns mockk<Notification>(relaxed = true)
        }

        notificationFactory = spyk(
            NotificationFactory(
                categoriesFactory,
                notificationStyleFactory,
                notificationBuilderFactory,
                iconProvider,
                pendingIntentProvider
            )
        )

        every { notificationFactory.setSmallIcon(any()) } answers {
            notificationBuilderFactory.setSmallIcon(1)
        }

        coEvery { notificationFactory.addButtons(any(), any(), any()) } coAnswers { Unit }
    }

    @Test
    fun testCreateSimpleNotification() = runBlocking {
        val notificationId = 123
        val builder = notificationBuilderFactory

        val notification = notificationFactory.createSimpleNotification(pushData, notificationId)

        Truth.assertThat(notification).isNotNull()

        coVerifyAll {
            builder.setPriority(any())
            builder.setContentTitle(any())
            builder.setContentText(any())
            builder.setStyle(any())
            builder.setAutoCancel(true)
            builder.setSmallIcon(any<Int>())
            builder.setContentIntent(any())
            builder.setDeleteIntent(any())
            builder.build()
        }
    }
}
