package com.appoxee.internal.migration

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.appoxee.internal.AppoxeeAdapter
import com.appoxee.internal.AppoxeeImpl
import com.appoxee.internal.TestDispatchersProvider
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.provider.ObserversProvider
import com.appoxee.internal.storage.InMemoryStorageImpl
import com.appoxee.shared.AppoxeeOptions
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * End-to-end migration test.
 *
 * Writes real v6 files and SharedPreferences, runs the real [AppoxeeImpl.validateRegistration]
 * migration path, and asserts that the v7 [InMemoryStorageImpl] contains the expected data.
 *
 * Only the network boundary ([AppoxeeAdapter.getDevice]) and Firebase ([AppoxeeImpl.updateOptStatus])
 * are mocked — everything else (file I/O, JSON parsing, storage writes) uses production code.
 */
class MigrationEndToEndTest {

    private lateinit var context: Context

    private val migrationFile get() = File(context.filesDir, "persistence_DeviceRealState.data")
    private val sharedPrefs get() = context.getSharedPreferences("appoxee", Context.MODE_PRIVATE)

    // v7 options — server URL must match AppoxeeOptions.Server.TEST
    private val v7Options = AppoxeeOptions(
        server = AppoxeeOptions.Server.TEST,
        sdkKey = "1111.2222",
        appId = "33333",
        tenantId = "44444"
    )

    // Fake device returned by the mocked adapter
    private val fakeDevice = DevicePayload(
        udidHashed = "hashed-abc",
        dmcUserId = "user123",
        pushToken = "fcm-token-123",
        alias = "user@test.com"
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        migrationFile.delete()
        sharedPrefs.edit().clear().commit()
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        migrationFile.delete()
        sharedPrefs.edit().clear().commit()
        unmockkAll()
        Dispatchers.resetMain()
    }

    // region helpers

    /**
     * Builds an [AppoxeeImpl] spy with:
     * - real [MigrationHelperImpl] (reads actual files and SharedPrefs)
     * - real [InMemoryStorageImpl] (lets us read written values back)
     * - spy on [AppoxeeAdapter] with [AppoxeeAdapter.getDevice] returning [fakeDevice]
     * - [AppoxeeImpl.updateOptStatus] stubbed to avoid Firebase
     *
     * [StandardTestDispatcher] is used so the coroutine launched by `init` does NOT run
     * automatically — spy overrides are guaranteed to be in place before any migration code runs.
     */
    private fun buildSut(
        storage: InMemoryStorageImpl,
        options: AppoxeeOptions = v7Options
    ): AppoxeeImpl {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        val dispatchers = TestDispatchersProvider(dispatcher, dispatcher, dispatcher)
        val realMigration = MigrationHelperImpl(context)
        val mockAdapter = spyk(AppoxeeAdapter(mockk(relaxed = true), storage)) {
            coEvery { getDevice() } returns fakeDevice
        }
        val sut = spyk(AppoxeeImpl(context as Application, options, dispatchers, ObserversProvider()))
        every { sut.storage } returns storage
        every { sut.migrationHelper } returns realMigration
        every { sut.appoxeeAdapter } returns mockAdapter
        coEvery { sut.updateOptStatus(any(), any()) } just runs
        return sut
    }

    private fun writeSameChannelV6Prefs() {
        sharedPrefs.edit()
            .putString("sdk_key", "1111.2222")
            .putString("cep_app_id", "33333")
            .putString("cep_tenant_id", "44444")
            .putString("server", "https://charon-test.shortest-route.com")
            .commit()
    }

    private fun writeDifferentChannelV6Prefs() {
        sharedPrefs.edit()
            .putString("sdk_key", "9999.0000")
            .putString("cep_app_id", "11111")
            .putString("cep_tenant_id", "22222")
            .putString("server", "https://charon-test.shortest-route.com")
            .commit()
    }

    // endregion

    // region tests

    @Test
    fun migration_sameChannel_tagsAndCustomAttributesMigratedToV7Storage() = runBlocking {
        // Write v6 state: alias, push opt-in, tags, and custom attributes
        val v6Json = """{"alias":"user@test.com","pushEnabled":true,"pushToken":"fcm-token-123","timestamp":1700000000,"tags":["sports","news"],"customAttributes":{"color":{"key":"color","value":"blue"},"score":{"key":"score","value":42}}}"""
        migrationFile.writeText("com.appoxee.internal.model.Device\n$v6Json", Charsets.UTF_8)
        writeSameChannelV6Prefs()

        val storage = InMemoryStorageImpl()
        val sut = buildSut(storage)

        sut.validateRegistration()

        // Tags migrated
        assertThat(storage.getTags()).containsExactly("sports", "news")

        // Custom attributes migrated with correct values
        val attrs = storage.getCustomAttributesCache().attributes
        assertThat(attrs).containsEntry("color", "blue")
        assertThat(attrs).containsEntry("score", 42)

        // Device payload saved (migration succeeded)
        assertThat(storage.getDevicePayload()?.udidHashed).isEqualTo("hashed-abc")

        // v6 data cleaned up
        assertThat(migrationFile.exists()).isFalse()
        assertThat(sharedPrefs.all).isEmpty()
    }

    @Test
    fun migration_differentChannel_tagsNotMigrated_v6DataDeleted() = runBlocking {
        // Write v6 state with a different channel than v7Options
        val v6Json = """{"alias":"user@test.com","pushEnabled":true,"pushToken":"fcm-token-123","timestamp":1700000000,"tags":["sports","news"],"customAttributes":{"color":{"key":"color","value":"blue"}}}"""
        migrationFile.writeText("com.appoxee.internal.model.Device\n$v6Json", Charsets.UTF_8)
        writeDifferentChannelV6Prefs()   // different sdk_key/appId — no same-channel migration

        val storage = InMemoryStorageImpl()
        val sut = buildSut(storage)

        sut.validateRegistration()

        // No tags or attributes written: channel changed → fresh registration path taken
        assertThat(storage.getTags()).isEmpty()
        assertThat(storage.getCustomAttributesCache().attributes).isEmpty()

        // v6 data cleaned up even on the re-registration path
        assertThat(migrationFile.exists()).isFalse()
        assertThat(sharedPrefs.all).isEmpty()
    }

    @Test
    fun migration_sameChannel_noTagsOrAttributes_devicePayloadSavedStorageRemainsEmpty() =
        runBlocking {
            // v6 file has no tags or customAttributes keys
            val v6Json = """{"alias":"user@test.com","pushEnabled":true,"pushToken":"fcm-token-123","timestamp":1700000000}"""
            migrationFile.writeText("com.appoxee.internal.model.Device\n$v6Json", Charsets.UTF_8)
            writeSameChannelV6Prefs()

            val storage = InMemoryStorageImpl()
            val sut = buildSut(storage)

            sut.validateRegistration()

            // Nothing to migrate — storage lists stay empty
            assertThat(storage.getTags()).isEmpty()
            assertThat(storage.getCustomAttributesCache().attributes).isEmpty()

            // Migration still completed — device payload saved
            assertThat(storage.getDevicePayload()?.udidHashed).isEqualTo("hashed-abc")

            // v6 data cleaned up
            assertThat(migrationFile.exists()).isFalse()
            assertThat(sharedPrefs.all).isEmpty()
        }

    // endregion
}
