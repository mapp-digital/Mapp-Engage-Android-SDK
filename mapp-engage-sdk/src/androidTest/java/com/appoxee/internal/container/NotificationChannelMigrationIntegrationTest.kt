package com.appoxee.internal.container

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
import com.appoxee.internal.storage.InMemoryStorageImpl
import com.appoxee.internal.ui.push.base.PushManager
import com.appoxee.internal.ui.push.model.PushData
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID
import kotlin.random.Random

/**
 * Uses real channels, PushContainer, PushManagerImpl, NotifyImpl and NotificationFactory.
 * The app container supplies only in-memory storage; no registration, Firebase or network is needed.
 * Channel settings are seeded as persisted upgrade state, without changing system settings via UI.
 * Recreating PushContainer checks resolution without its previous lazy state, not an APK/process restart.
 */
@SdkSuppress(minSdkVersion = 26)
class NotificationChannelMigrationIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = NotificationManagerCompat.from(context)
    private val prefix = "mapp-migration-test-${UUID.randomUUID()}"
    private val v7Id = "$prefix-v7"
    private val legacyId = "$prefix-v6"
    private val storage = InMemoryStorageImpl()
    private val appContainer = mockk<AppoxeeContainer> {
        every { storage } returns this@NotificationChannelMigrationIntegrationTest.storage
    }
    private val notifications = mutableListOf<Pair<Int, Notification>>()
    private var nextNotificationId = Random.nextInt(1, Int.MAX_VALUE / 2)

    @After
    fun tearDown() {
        notifications.forEach { (id, notification) ->
            manager.cancel(id)
            notification.contentIntent?.cancel()
            notification.deleteIntent?.cancel()
        }
        manager.deleteNotificationChannel(v7Id)
        manager.deleteNotificationChannel(legacyId)
    }

    @Test
    fun freshInstallCreatesHighChannelAndBuildsNotificationsOnIt() = runBlocking {
        assertThat(manager.getNotificationChannel(v7Id)).isNull()
        assertThat(manager.getNotificationChannel(legacyId)).isNull()

        val container = newContainer()
        assertUsesChannel(container, v7Id)
        val created = checkNotNull(manager.getNotificationChannel(v7Id))
        assertThat(created.importance).isEqualTo(NotificationManager.IMPORTANCE_HIGH)

        assertUsesChannel(container, v7Id)
        assertUsesChannel(newContainer(), v7Id)
        assertThat(manager.getNotificationChannel(v7Id)).isEqualTo(created)
        assertThat(manager.getNotificationChannel(legacyId)).isNull()
    }

    @Test
    fun directV6UpgradeReusesHighLegacyChannel() = runBlocking {
        assertDirectUpgradePreservesLegacy(NotificationManager.IMPORTANCE_HIGH)
    }

    @Test
    fun directV6UpgradePreservesLowImportanceAndOtherLegacyPreferences() = runBlocking {
        assertDirectUpgradePreservesLegacy(NotificationManager.IMPORTANCE_LOW)
    }

    @Test
    fun directV6UpgradeKeepsBlockedLegacyChannelWithoutCreatingV7() = runBlocking {
        assertDirectUpgradePreservesLegacy(NotificationManager.IMPORTANCE_NONE)
    }

    @Test
    fun alreadyMigratedInstallKeepsDefaultV7InsteadOfHighLegacy() = runBlocking {
        assertAlreadyMigratedPreservesV7(NotificationManager.IMPORTANCE_DEFAULT)
    }

    @Test
    fun alreadyMigratedInstallKeepsBlockedV7InsteadOfHighLegacy() = runBlocking {
        assertAlreadyMigratedPreservesV7(NotificationManager.IMPORTANCE_NONE)
    }

    @Test
    fun existingV7OnlyInstallKeepsDefaultChannel() = runBlocking {
        val before = seedChannel(v7Id, NotificationManager.IMPORTANCE_DEFAULT)

        val container = newContainer()
        assertUsesChannel(container, v7Id)
        assertUsesChannel(container, v7Id)
        assertUsesChannel(newContainer(), v7Id)

        assertThat(manager.getNotificationChannel(v7Id)).isEqualTo(before)
        assertThat(manager.getNotificationChannel(legacyId)).isNull()
    }

    @Test
    fun postedNotificationUsesTheSelectedLegacyChannelWhenPermissionIsGranted() = runBlocking {
        assumeTrue("Enable notifications for the test app to run the posting check", manager.areNotificationsEnabled())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            assumeTrue(
                "Grant POST_NOTIFICATIONS to the test app to run the posting check",
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
            )
        }
        val before = seedChannel(legacyId, NotificationManager.IMPORTANCE_HIGH)
        val pushManager = newContainer().pushManager
        val (notificationId, notification) = buildNotification(pushManager)

        assertThat(notification.channelId).isEqualTo(legacyId)
        pushManager.showNotification(notification, notificationId)

        val platformManager = checkNotNull(context.getSystemService(NotificationManager::class.java))
        val posted = withTimeout(5_000) {
            var found = platformManager.activeNotifications.firstOrNull { it.id == notificationId }
            while (found == null) {
                delay(50)
                found = platformManager.activeNotifications.firstOrNull { it.id == notificationId }
            }
            checkNotNull(found)
        }
        assertThat(posted.notification.channelId).isEqualTo(legacyId)
        assertThat(manager.getNotificationChannel(legacyId)).isEqualTo(before)
        assertThat(manager.getNotificationChannel(v7Id)).isNull()
    }

    private fun newContainer() = PushContainer(context, appContainer, v7Id, legacyId)

    private suspend fun assertDirectUpgradePreservesLegacy(importance: Int) {
        val before = seedChannel(legacyId, importance)
        val container = newContainer()

        assertUsesChannel(container, legacyId)
        assertUsesChannel(container, legacyId)
        assertUsesChannel(newContainer(), legacyId)

        assertThat(manager.getNotificationChannel(legacyId)).isEqualTo(before)
        assertThat(manager.getNotificationChannel(v7Id)).isNull()
    }

    private suspend fun assertAlreadyMigratedPreservesV7(importance: Int) {
        val legacyBefore = seedChannel(legacyId, NotificationManager.IMPORTANCE_HIGH)
        val v7Before = seedChannel(v7Id, importance)
        val container = newContainer()

        assertUsesChannel(container, v7Id)
        assertUsesChannel(container, v7Id)
        assertUsesChannel(newContainer(), v7Id)

        assertThat(manager.getNotificationChannel(v7Id)).isEqualTo(v7Before)
        assertThat(manager.getNotificationChannel(legacyId)).isEqualTo(legacyBefore)
    }

    private fun seedChannel(id: String, importance: Int): NotificationChannel {
        manager.createNotificationChannel(NotificationChannel(id, "Existing channel name", importance).apply {
            description = "Existing description"
            setSound(null, null)
            vibrationPattern = longArrayOf(0, 100, 100, 100)
            enableVibration(true)
            setShowBadge(false)
        })
        return checkNotNull(manager.getNotificationChannel(id)).also {
            assertThat(it.importance).isEqualTo(importance)
        }
    }

    private suspend fun assertUsesChannel(container: PushContainer, expectedId: String) {
        val pushManager = container.pushManager
        val (_, notification) = buildNotification(pushManager)

        val channelId = checkNotNull(notification.channelId)
        assertThat(channelId).isEqualTo(expectedId)
        assertThat(manager.getNotificationChannel(channelId)).isNotNull()
        assertThat(notification.extras.getString(Notification.EXTRA_TITLE)).isEqualTo("Channel migration")
    }

    private suspend fun buildNotification(pushManager: PushManager): Pair<Int, Notification> {
        val id = nextNotificationId++
        val notification = pushManager.createNotification(
            PushData(id = id.toLong(), title = "Channel migration", bigText = "Integration test", type = "text"),
            id,
        )
        return (id to notification).also { notifications.add(it) }
    }
}
