package com.appoxee.internal.migration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.appoxee.shared.AppoxeeOptions
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class MigrationHelperImplTest {

    private lateinit var context: Context
    private lateinit var sut: MigrationHelperImpl

    private val migrationFile get() = File(context.filesDir, "persistence_DeviceRealState.data")
    private val sharedPrefs get() = context.getSharedPreferences("appoxee", Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sut = MigrationHelperImpl(context)
        migrationFile.delete()
        sharedPrefs.edit().clear().commit()
    }

    @After
    fun tearDown() {
        migrationFile.delete()
        sharedPrefs.edit().clear().commit()
    }

    // region fetchRegistrationData

    @Test
    fun fetchRegistrationData_parsesValidV6File() = runBlocking {
        val json = """{"alias":"user@test.com","pushEnabled":true,"pushToken":"fcm-token-123","timestamp":1700000000}"""
        migrationFile.writeText("com.appoxee.internal.model.Device\n$json", Charsets.UTF_8)

        val result = sut.fetchRegistrationData()

        assertThat(result).isNotNull()
        assertThat(result!!.alias).isEqualTo("user@test.com")
        assertThat(result.pushEnabled).isTrue()
        assertThat(result.pushToken).isEqualTo("fcm-token-123")
        assertThat(result.timestamp).isEqualTo(1700000000L)
    }

    @Test
    fun fetchRegistrationData_parsesFileWithOptOutToken() = runBlocking {
        val json = """{"alias":"user@test.com","pushEnabled":false,"pushToken":"","timestamp":1700000001}"""
        migrationFile.writeText("com.appoxee.internal.model.Device\n$json", Charsets.UTF_8)

        val result = sut.fetchRegistrationData()

        assertThat(result).isNotNull()
        assertThat(result!!.pushEnabled).isFalse()
        assertThat(result.pushToken).isEmpty()
    }

    @Test
    fun fetchRegistrationData_returnsNullWhenFileDoesNotExist() = runBlocking {
        val result = sut.fetchRegistrationData()
        assertThat(result).isNull()
    }

    @Test
    fun fetchRegistrationData_returnsNullWhenFileContainsOnlyClassPrefix() = runBlocking {
        migrationFile.writeText("com.appoxee.internal.model.Device", Charsets.UTF_8)

        val result = sut.fetchRegistrationData()
        assertThat(result).isNull()
    }

    @Test
    fun fetchRegistrationData_returnsNullOnMalformedJson() = runBlocking {
        migrationFile.writeText("com.appoxee.internal.model.Device\nnot{{valid--json", Charsets.UTF_8)

        val result = sut.fetchRegistrationData()
        // exception caught internally — must return null rather than crash
        assertThat(result).isNull()
    }

    @Test
    fun fetchRegistrationData_returnsNullOnEmptyFile() = runBlocking {
        migrationFile.writeText("", Charsets.UTF_8)

        val result = sut.fetchRegistrationData()
        assertThat(result).isNull()
    }

    // endregion

    // region getRegistrationOptions

    @Test
    fun getRegistrationOptions_returnsCorrectOptionsFromSharedPrefs() = runBlocking {
        sharedPrefs.edit()
            .putString("sdk_key", "1111.2222")
            .putString("cep_app_id", "33333")
            .putString("cep_tenant_id", "44444")
            .putString("server", "https://charon-test.shortest-route.com")
            .commit()

        val result = sut.getRegistrationOptions()

        assertThat(result).isNotNull()
        assertThat(result!!.sdkKey).isEqualTo("1111.2222")
        assertThat(result.appId).isEqualTo("33333")
        assertThat(result.tenantId).isEqualTo("44444")
        assertThat(result.server).isEqualTo(AppoxeeOptions.Server.TEST)
    }

    @Test
    fun getRegistrationOptions_returnsNullWhenServerValueIsUnrecognised() = runBlocking {
        sharedPrefs.edit()
            .putString("sdk_key", "key")
            .putString("cep_app_id", "app")
            .putString("cep_tenant_id", "tenant")
            .putString("server", "https://unknown.example.com")
            .commit()

        val result = sut.getRegistrationOptions()
        assertThat(result).isNull()
    }

    @Test
    fun getRegistrationOptions_returnsNullWhenSharedPrefsAreEmpty() = runBlocking {
        val result = sut.getRegistrationOptions()
        assertThat(result).isNull()
    }

    @Test
    fun getRegistrationOptions_returnsNullWhenServerKeyIsMissing() = runBlocking {
        sharedPrefs.edit()
            .putString("sdk_key", "key")
            .putString("cep_app_id", "app")
            .putString("cep_tenant_id", "tenant")
            // no "server" key
            .commit()

        val result = sut.getRegistrationOptions()
        assertThat(result).isNull()
    }

    // endregion

    // region deleteOldRegistration

    @Test
    fun deleteOldRegistration_removesFileAndClearsSharedPrefs() = runBlocking {
        migrationFile.writeText("com.appoxee.internal.model.Device\n{}", Charsets.UTF_8)
        sharedPrefs.edit().putString("sdk_key", "key").commit()

        sut.deleteOldRegistration()

        assertThat(migrationFile.exists()).isFalse()
        assertThat(sharedPrefs.all).isEmpty()
    }

    @Test
    fun deleteOldRegistration_doesNotThrowWhenFileDoesNotExist() = runBlocking {
        // file already absent — must not throw
        sut.deleteOldRegistration()
        assertThat(migrationFile.exists()).isFalse()
    }

    @Test
    fun deleteOldRegistration_clearsSharedPrefsEvenWhenFileAbsent() = runBlocking {
        sharedPrefs.edit().putString("sdk_key", "leftover_key").commit()

        sut.deleteOldRegistration()

        assertThat(sharedPrefs.all).isEmpty()
    }

    // endregion
}
