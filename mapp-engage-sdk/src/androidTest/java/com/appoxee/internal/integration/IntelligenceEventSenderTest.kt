package com.appoxee.internal.integration

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class IntelligenceEventSenderTest {

    private lateinit var context: Context
    private lateinit var applicationContext: Context
    private lateinit var packageManager: PackageManager
    private lateinit var sender: IntelligenceEventSender

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        applicationContext = mockk(relaxed = true)
        packageManager = mockk(relaxed = true)

        every { context.applicationContext } returns applicationContext
        every { applicationContext.packageName } returns APP_PACKAGE
        every { applicationContext.packageManager } returns packageManager

        sender = AndroidIntelligenceEventSender(context)
    }

    @Test
    fun sendsPackageScopedExplicitBroadcastWithDmcUserId() {
        stubReceivers(resolveInfo(RECEIVER_NAME))
        val sentIntent = slot<Intent>()
        every { applicationContext.sendBroadcast(capture(sentIntent)) } returns Unit

        sender.sendDmcUserId(DMC_USER_ID)

        assertThat(sentIntent.captured.action).isEqualTo(ACTION_NAME)
        assertThat(sentIntent.captured.`package`).isEqualTo(APP_PACKAGE)
        assertThat(sentIntent.captured.component?.packageName).isEqualTo(APP_PACKAGE)
        assertThat(sentIntent.captured.component?.className).isEqualTo(RECEIVER_NAME)
        assertThat(sentIntent.captured.getStringExtra(DMC_USER_ID_KEY)).isEqualTo(DMC_USER_ID)
    }

    @Test
    fun sendsOneBroadcastPerResolvedReceiver() {
        stubReceivers(resolveInfo(RECEIVER_NAME), resolveInfo(SECOND_RECEIVER_NAME))

        sender.sendDmcUserId(DMC_USER_ID)

        verify(exactly = 2) { applicationContext.sendBroadcast(any()) }
    }

    @Test
    fun doesNotSendWhenNoReceiverIsResolved() {
        stubReceivers()

        sender.sendDmcUserId(DMC_USER_ID)

        verify(exactly = 0) { applicationContext.sendBroadcast(any()) }
    }

    @Test
    fun doesNotQueryOrSendForEmptyDmcUserId() {
        sender.sendDmcUserId("")

        verify(exactly = 0) { applicationContext.packageManager }
        verify(exactly = 0) { applicationContext.sendBroadcast(any()) }
    }

    @Test
    fun packageManagerFailureDoesNotEscape() {
        stubReceiverFailure()

        sender.sendDmcUserId(DMC_USER_ID)

        verify(exactly = 0) { applicationContext.sendBroadcast(any()) }
    }

    @Test
    fun broadcastFailureDoesNotEscape() {
        stubReceivers(resolveInfo(RECEIVER_NAME))
        every { applicationContext.sendBroadcast(any()) } throws IllegalStateException("failure")

        sender.sendDmcUserId(DMC_USER_ID)

        verify(exactly = 1) { applicationContext.sendBroadcast(any()) }
    }

    private fun resolveInfo(receiverName: String): ResolveInfo = ResolveInfo().apply {
        activityInfo = ActivityInfo().apply {
            packageName = APP_PACKAGE
            name = receiverName
        }
    }

    private fun stubReceivers(vararg receivers: ResolveInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            every {
                packageManager.queryBroadcastReceivers(
                    any(),
                    any<PackageManager.ResolveInfoFlags>(),
                )
            } returns receivers.toList()
        } else {
            @Suppress("DEPRECATION")
            every { packageManager.queryBroadcastReceivers(any(), any<Int>()) } returns receivers.toList()
        }
    }

    private fun stubReceiverFailure() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            every {
                packageManager.queryBroadcastReceivers(
                    any(),
                    any<PackageManager.ResolveInfoFlags>(),
                )
            } throws IllegalStateException("failure")
        } else {
            @Suppress("DEPRECATION")
            every {
                packageManager.queryBroadcastReceivers(any(), any<Int>())
            } throws IllegalStateException("failure")
        }
    }

    private companion object {
        const val ACTION_NAME = "webtrekk.android.sdk.integration.MappIntelligenceListener"
        const val DMC_USER_ID_KEY = "dmcUserId"
        const val DMC_USER_ID = "user12345"
        const val APP_PACKAGE = "com.example.app"
        const val RECEIVER_NAME = "webtrekk.android.sdk.integration.EngageIntegrationReceiver"
        const val SECOND_RECEIVER_NAME = "webtrekk.android.sdk.integration.SecondReceiver"
    }
}
