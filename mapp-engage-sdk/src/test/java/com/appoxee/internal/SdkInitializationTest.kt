package com.appoxee.internal

import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.NotificationMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SdkInitializationTest {
    private fun options() = AppoxeeOptions(AppoxeeOptions.Server.TEST, "key", "app", "tenant")

    @Test
    fun `same configuration and no-options calls reuse both initializing and ready SDK`() {
        val initialization = SdkInitialization(options())
        assertThat(initialization.canReuse(options())).isTrue()
        assertThat(initialization.canReuse(null)).isTrue()
        initialization.complete(ready = true)
        assertThat(initialization.canReuse(options())).isTrue()
        assertThat(initialization.canReuse(null)).isTrue()
    }

    @Test
    fun `failed initialization is retryable regardless of observer registration`() {
        val initialization = SdkInitialization(options())
        initialization.complete(ready = false)
        assertThat(initialization.canReuse(options())).isFalse()
        assertThat(initialization.canReuse(null)).isFalse()
        assertThat(initialization.configuration!!.sdkKey).isEqualTo("key")
    }

    @Test
    fun `capture mutable options and detect settings omitted by public equals`() {
        val original = options()
        val initialization = SdkInitialization(original)
        original.notificationMode = NotificationMode.SILENT_ONLY
        assertThat(initialization.canReuse(original)).isFalse()
        assertThat(initialization.canReuse(options())).isTrue()
        val logging = options().apply { logType = AppoxeeOptions.LogLevel.RELEASE }
        assertThat(initialization.canReuse(logging)).isFalse()
        val timeouts = options().apply { readTimeout = 30_000 }
        assertThat(initialization.canReuse(timeouts)).isFalse()
    }

    @Test
    fun `resolved saved configuration allows equivalent explicit engagement`() {
        val initialization = SdkInitialization(null)
        assertThat(initialization.canReuse(null)).isTrue()
        assertThat(initialization.canReuse(options())).isFalse()
        initialization.resolveSavedConfiguration(options())
        assertThat(initialization.canReuse(options())).isTrue()
        val alias = AppoxeeOptions(AppoxeeOptions.Server.STAGING, "key", "app", "tenant")
        assertThat(initialization.canReuse(alias)).isTrue()
    }

    @Test
    fun `saved configuration cannot overwrite explicit configuration`() {
        val initialization = SdkInitialization(options())
        initialization.resolveSavedConfiguration(AppoxeeOptions(AppoxeeOptions.Server.L3, "old", "app", "tenant"))
        assertThat(initialization.canReuse(options())).isTrue()
    }

    @Test
    fun `channel identity ignores notification and logging settings but includes credentials`() {
        val first = SdkInitialization(options())
        val settings = SdkInitialization(options().apply {
            notificationMode = NotificationMode.SILENT_ONLY
            logType = AppoxeeOptions.LogLevel.RELEASE
        })
        assertThat(first.hasSameChannel(settings)).isTrue()
        assertThat(first.hasSameChannel(SdkInitialization(
            AppoxeeOptions(AppoxeeOptions.Server.TEST, "other", "app", "tenant")
        ))).isFalse()
        assertThat(SdkInitialization(null).hasSameChannel(first)).isFalse()
    }
}
