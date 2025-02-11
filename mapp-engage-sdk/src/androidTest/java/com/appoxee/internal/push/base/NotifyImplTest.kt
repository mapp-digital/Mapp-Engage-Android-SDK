package com.appoxee.internal.push.base

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.appoxee.internal.ui.push.base.Notify
import com.appoxee.internal.ui.push.base.NotifyImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class NotifyImplTest {
    private lateinit var context: Context

    private lateinit var notify: Notify

    private lateinit var notificationManager: NotificationManagerCompat

    @Before
    fun setUp() {
        context = mockk(relaxed = true) {
            every { checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) } returns PackageManager.PERMISSION_GRANTED
        }
        notificationManager = mockk(relaxed = true, relaxUnitFun = true)
        notify = spyk(NotifyImpl(context, notificationManager))
    }


    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun showNotification() {
        val notification = mockk<Notification>()
        notify.showNotification(notification, 1)
        verify { notificationManager.notify(1, notification) }
    }

    @Test
    fun closeNotification() {
        notify.closeNotification(1)
        verify { notificationManager.cancel(1) }
    }

    @Test
    fun createChannel() {
        val channelId = "Mapp_Notification_channel_1"
        val channelName = "Base Notification Channel"
        val importance = NotificationCompat.PRIORITY_HIGH
        val channel = NotificationChannel(channelId, channelName, importance)
        notify.createChannel(channelId, channelName, importance)
        verify {
            notificationManager.createNotificationChannel(channel)
        }
    }
}