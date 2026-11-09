package com.appoxee.internal.container

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import java.util.UUID

@SdkSuppress(minSdkVersion = 26)
class NotificationChannelSelectionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = NotificationManagerCompat.from(context)
    private val createdIds = mutableListOf<String>()
    private val importanceLevels = listOf(
        NotificationManager.IMPORTANCE_HIGH,
        NotificationManager.IMPORTANCE_DEFAULT,
        NotificationManager.IMPORTANCE_LOW,
        NotificationManager.IMPORTANCE_NONE,
    )

    @After
    fun tearDown() {
        createdIds.forEach { manager.deleteNotificationChannel(it) }
    }

    @Test
    fun noChannelsSelectsV7WithoutCreatingIt() {
        val v7Id = newId()
        val legacyId = newId()

        assertThat(resolveNotificationChannelId(manager, v7Id, legacyId)).isEqualTo(v7Id)
        assertThat(manager.getNotificationChannel(v7Id)).isNull()
        assertThat(manager.getNotificationChannel(legacyId)).isNull()
    }

    @Test
    fun legacyOnlyKeepsItsIdAndSettingsAtEveryImportance() {
        importanceLevels.forEach { importance ->
            val v7Id = newId()
            val legacyId = createChannel(importance)
            val before = checkNotNull(manager.getNotificationChannel(legacyId))

            repeat(2) {
                // A fresh manager models re-resolving without a stored migration decision.
                val freshManager = NotificationManagerCompat.from(context)
                assertThat(resolveNotificationChannelId(freshManager, v7Id, legacyId))
                    .isEqualTo(legacyId)
                assertThat(manager.getNotificationChannel(v7Id)).isNull()
                assertThat(manager.getNotificationChannel(legacyId)).isEqualTo(before)
            }
        }
    }

    @Test
    fun v7OnlyKeepsItsIdAndSettingsAtEveryImportance() {
        importanceLevels.forEach { importance ->
            val v7Id = createChannel(importance)
            val legacyId = newId()
            val before = checkNotNull(manager.getNotificationChannel(v7Id))

            repeat(2) {
                assertThat(resolveNotificationChannelId(manager, v7Id, legacyId)).isEqualTo(v7Id)
                assertThat(manager.getNotificationChannel(v7Id)).isEqualTo(before)
                assertThat(manager.getNotificationChannel(legacyId)).isNull()
            }
        }
    }

    @Test
    fun existingV7WinsOverHighLegacyEvenWhenV7IsBlocked() {
        importanceLevels.forEach { importance ->
            val v7Id = createChannel(importance)
            val legacyId = createChannel(NotificationManager.IMPORTANCE_HIGH)
            val beforeV7 = checkNotNull(manager.getNotificationChannel(v7Id))
            val beforeLegacy = checkNotNull(manager.getNotificationChannel(legacyId))

            repeat(2) {
                assertThat(resolveNotificationChannelId(manager, v7Id, legacyId)).isEqualTo(v7Id)
                assertThat(manager.getNotificationChannel(v7Id)).isEqualTo(beforeV7)
                assertThat(manager.getNotificationChannel(legacyId)).isEqualTo(beforeLegacy)
            }
        }
    }

    private fun newId(): String = "mapp-selection-test-${UUID.randomUUID()}"

    private fun createChannel(importance: Int): String {
        val id = newId()
        createdIds.add(id)
        manager.createNotificationChannel(NotificationChannel(id, "Existing channel", importance).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        })
        assertThat(checkNotNull(manager.getNotificationChannel(id)).importance).isEqualTo(importance)
        return id
    }
}
