package com.mapp.engagesample.configuration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.NotificationMode
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SdkConfigurationPreferencesTest {
    // Separate from the application's actual SDK settings and native SDK storage.
    private val preferences = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("sdk_configuration_storage_test", Context.MODE_PRIVATE)
    private val storage = SdkConfigurationPreferences(preferences)

    @Before
    fun setUp() { preferences.edit().clear().commit() }

    @After
    fun tearDown() { preferences.edit().clear().commit() }

    @Test
    fun savesEveryFieldAndLoadsItFromANewStorageOwner() = runBlocking {
        val configuration = SdkConfiguration(
            AppoxeeOptions.Server.TEST_61, "fixture-key", "fixture-app", "fixture-tenant",
            NotificationMode.SILENT_ONLY, AppoxeeOptions.LogLevel.RELEASE,
        )
        storage.save(configuration)
        assertThat(SdkConfigurationPreferences(preferences).load()).isEqualTo(configuration)
        storage.save(null)
        assertThat(SdkConfigurationPreferences(preferences).load()).isNull()
    }

    @Test
    fun invalidSavedEnumAndWrongPreferenceTypesFallBackToDefaults() {
        preferences.edit().putString("sdk_key", "fixture-key").putString("server", "UNKNOWN").commit()
        assertThat(storage.load()).isNull()
        preferences.edit().putInt("sdk_key", 123).commit()
        assertThat(storage.load()).isNull()
    }
}
