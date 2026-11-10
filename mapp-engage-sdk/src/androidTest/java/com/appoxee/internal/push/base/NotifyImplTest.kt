package com.appoxee.internal.push.base

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.appoxee.internal.ui.push.base.Notify
import com.appoxee.internal.ui.push.base.NotifyImpl
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID

class NotifyImplTest {
    private lateinit var context: Context

    private lateinit var notify: Notify

    private lateinit var notificationManager: NotificationManagerCompat
    private val createdIds = mutableListOf<String>()
    private val realContext = ApplicationProvider.getApplicationContext<Context>()
    private val realManager = NotificationManagerCompat.from(realContext)

    @Before
    fun setUp() {
        context = mockk(relaxed = true) {
            every { checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) } returns PackageManager.PERMISSION_GRANTED
        }
        notificationManager = mockk(relaxed = true, relaxUnitFun = true)
        every { notificationManager.getNotificationChannel(any()) } returns null
        notify = spyk(NotifyImpl(context, notificationManager))
    }


    @After
    fun tearDown() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createdIds.forEach { realManager.deleteNotificationChannel(it) }
        }
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
    @SdkSuppress(minSdkVersion = 26)
    fun createChannel() {
        val channelId = "Mapp_Notification_channel_1"
        val channelName = "Base Notification Channel"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelId, channelName, importance)
        notify.createChannel(channelId, channelName, importance)
        verify {
            notificationManager.createNotificationChannel(channel)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun existingChannelIsNotRegisteredAgain() {
        val channelId = "existing-channel"
        val existing = NotificationChannel(channelId, "User's channel", NotificationManager.IMPORTANCE_LOW)
        every { notificationManager.getNotificationChannel(channelId) } returns existing

        repeat(2) {
            notify.createChannel(channelId, "SDK name", NotificationManager.IMPORTANCE_HIGH)
        }

        verify(exactly = 0) { notificationManager.createNotificationChannel(any<NotificationChannel>()) }
    }

    @Test
    @SdkSuppress(maxSdkVersion = 25)
    fun channelCreationBelowAndroidOIsANoOp() {
        notify.createChannel("channel-id", "Channel name", NotificationManager.IMPORTANCE_HIGH)

        verify(exactly = 0) { notificationManager.getNotificationChannel(any()) }
        verify(exactly = 0) { notificationManager.createNotificationChannel(any<NotificationChannel>()) }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun absentChannelIsCreatedAtHighAndRepeatedCallsPreserveIt() {
        val channelId = newRealChannelId()
        val realNotify = NotifyImpl(realContext, realManager)

        realNotify.createChannel(channelId, "Initial channel", NotificationManager.IMPORTANCE_HIGH)
        val created = checkNotNull(realManager.getNotificationChannel(channelId))
        assertThat(created.importance).isEqualTo(NotificationManager.IMPORTANCE_HIGH)

        repeat(2) {
            realNotify.createChannel(channelId, "Replacement name", NotificationManager.IMPORTANCE_DEFAULT)
        }

        assertThat(realManager.getNotificationChannel(channelId)).isEqualTo(created)
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun precreatedChannelSettingsArePreservedAtEveryImportance() {
        val realNotify = NotifyImpl(realContext, realManager)
        listOf(
            NotificationManager.IMPORTANCE_HIGH,
            NotificationManager.IMPORTANCE_DEFAULT,
            NotificationManager.IMPORTANCE_LOW,
            NotificationManager.IMPORTANCE_NONE,
        ).forEach { importance ->
            val channelId = newRealChannelId()
            realManager.createNotificationChannel(NotificationChannel(channelId, "Existing name", importance).apply {
                setSound(null, null)
                vibrationPattern = longArrayOf(0, 100, 100, 100)
                enableVibration(true)
                setShowBadge(false)
            })
            val before = checkNotNull(realManager.getNotificationChannel(channelId))
            assertThat(before.importance).isEqualTo(importance)

            repeat(2) {
                realNotify.createChannel(channelId, "SDK name", NotificationManager.IMPORTANCE_HIGH)
            }

            val after = checkNotNull(realManager.getNotificationChannel(channelId))
            assertThat(after.importance).isEqualTo(before.importance)
            assertThat(after.name.toString()).isEqualTo(before.name.toString())
            assertThat(after.sound).isEqualTo(before.sound)
            assertThat(after.audioAttributes).isEqualTo(before.audioAttributes)
            assertThat(after.shouldVibrate()).isEqualTo(before.shouldVibrate())
            assertThat(after.vibrationPattern).isEqualTo(before.vibrationPattern)
            assertThat(after.canShowBadge()).isEqualTo(before.canShowBadge())
            assertThat(after).isEqualTo(before)
        }
    }

    private fun newRealChannelId(): String = "mapp-notify-test-${UUID.randomUUID()}".also {
        createdIds.add(it)
    }
}
