package com.appoxee.internal.util

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class CompatExtTest {
    @Test
    fun notificationPendingIntentRejectsInjectedFieldsOnAllSupportedAndroidVersions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val action = "${context.packageName}.test.immutable.${UUID.randomUUID()}"
        val delivered = AtomicReference<Intent>()
        val received = CountDownLatch(1)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                delivered.set(intent)
                received.countDown()
            }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(action),
            ContextCompat.RECEIVER_NOT_EXPORTED)
        val pendingIntent = PendingIntent.getBroadcast(context, 0,
            Intent(action).setPackage(context.packageName),
            CompatExt.PENDING_INTENT_UPDATE_CURRENT_FLAGS)
        try {
            pendingIntent.send(context, 0, Intent().apply {
                data = android.net.Uri.parse("https://attacker.example.com")
                putExtra("injected", "untrusted")
            })

            assertThat(received.await(5, TimeUnit.SECONDS)).isTrue()
            assertThat(delivered.get().data).isNull()
            assertThat(delivered.get().getStringExtra("injected")).isNull()
        } finally {
            pendingIntent.cancel()
            context.unregisterReceiver(receiver)
        }
    }
}
