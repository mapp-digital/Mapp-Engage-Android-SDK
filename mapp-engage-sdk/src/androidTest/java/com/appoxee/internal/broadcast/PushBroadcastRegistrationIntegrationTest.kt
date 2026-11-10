package com.appoxee.internal.broadcast

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.appoxee.internal.AppoxeeImpl
import com.appoxee.internal.TestDispatchersProvider
import com.appoxee.internal.container.LifecycleCallbacksRegistration
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.storage.InMemoryStorageImpl
import com.appoxee.internal.ui.push.model.PushData
import com.appoxee.internal.util.Logger
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.LocalPushBroadcast
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Exercises actual registration and dispatch with real Android Intents and in-memory storage.
 * The Context records outgoing broadcasts; this does not test OS delivery to a manifest receiver.
 * SDK initialization is paused at storage so registration runs in its real, active SDK scope.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PushBroadcastRegistrationIntegrationTest {
    private val realContext = ApplicationProvider.getApplicationContext<Context>()
    private val storage = spyk(InMemoryStorageImpl())
    private val receiverMutex = Mutex()
    private var registeredClass: Class<*>? = null
    private val container = mockk<AppoxeeContainer>(relaxed = true)
    private val application = mockk<Application>(relaxed = true)
    private val callbackContext = mockk<Context>(relaxed = true)
    private val sent = slot<Intent>()
    private val receiver = MappInternalBroadcastReceiver()
    private val dispatcher = StandardTestDispatcher()
    private val options = AppoxeeOptions(AppoxeeOptions.Server.TEST_55, "key", "app", "tenant")
    private val initializationOptions = CompletableDeferred<AppoxeeOptions?>()
    private val sdks = mutableListOf<AppoxeeImpl>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { application.applicationInfo } returns realContext.applicationInfo
        every { application.applicationContext } returns realContext
        Logger.init(application, AppoxeeOptions.LogLevel.DEBUG)
        every { container.storage } returns storage
        every { container.lifecycleCallbacksRegistration } returns LifecycleCallbacksRegistration()
        every { container.initializationMutex } returns Mutex()
        every { container.pushBroadcastMutex } returns receiverMutex
        every { container.localPushBroadcast } answers { registeredClass }
        every { container.localPushBroadcast = any() } answers { registeredClass = firstArg() }
        // Stop bootstrap before configuration clearing or network work, without cancelling
        // the scope also used by setPushBroadcast().
        coEvery { storage.getInitOptions() } coAnswers { initializationOptions.await() }
        every { callbackContext.packageName } returns realContext.packageName
        every { callbackContext.applicationContext } returns realContext
        every { callbackContext.sendBroadcast(capture(sent)) } just runs
        receiver.setAppoxeeContainer(container)
    }

    @After
    fun tearDown() {
        sdks.forEach { it.internalScope.cancel() }
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun callbackTargetsRegisteredReceiverWhilePersistenceIsSuspended() = runTest {
        val writeStarted = CompletableDeferred<Unit>()
        val releaseWrite = CompletableDeferred<Unit>()
        coEvery { storage.setBroadcastClass(any()) } coAnswers {
            writeStarted.complete(Unit)
            releaseWrite.await()
        }
        val sdk = newSdk()

        sdk.setPushBroadcast(FirstReceiver::class.java)
        runCurrent()
        assertThat(writeStarted.isCompleted).isTrue()
        assertThat(storage.getBroadcastClass()).isNull()

        receiver.notifyClientApp(callbackContext, PushData(id = 1), LocalPushBroadcast.PUSH_RECEIVED)

        assertTargets(FirstReceiver::class.java)
        coVerify(exactly = 1) { storage.getBroadcastClass() } // Only the fixture's assertion read storage.
        assertThat(releaseWrite.isCompleted).isFalse()
        releaseWrite.complete(Unit)
        runCurrent()
    }

    @Test
    fun registrationDuringStorageLookupWinsOverTheStaleStoredReceiver() = runTest {
        storage.setBroadcastClass(FirstReceiver::class.java)
        val readStarted = CompletableDeferred<Unit>()
        val releaseRead = CompletableDeferred<Unit>()
        coEvery { storage.getBroadcastClass() } coAnswers {
            readStarted.complete(Unit)
            releaseRead.await()
            FirstReceiver::class.java
        }
        val sdk = newSdk()
        val dispatch = launch {
            receiver.notifyClientApp(callbackContext, PushData(id = 2), LocalPushBroadcast.PUSH_RECEIVED)
        }
        runCurrent()
        assertThat(readStarted.isCompleted).isTrue()

        sdk.setPushBroadcast(SecondReceiver::class.java)
        releaseRead.complete(Unit)
        dispatch.join()

        assertTargets(SecondReceiver::class.java)
        coVerify(exactly = 1) { storage.getBroadcastClass() }
    }

    @Test
    fun persistedReceiverIsUsedWhenThereIsNoInMemoryRegistration() = runTest {
        storage.setBroadcastClass(FirstReceiver::class.java)
        assertThat(registeredClass).isNull()

        receiver.notifyClientApp(callbackContext, PushData(id = 3), LocalPushBroadcast.PUSH_RECEIVED)

        assertTargets(FirstReceiver::class.java)
        coVerify(exactly = 1) { storage.getBroadcastClass() }
    }

    private fun newSdk(): AppoxeeImpl {
        val sdk = AppoxeeImpl(
            application,
            options,
            TestDispatchersProvider(dispatcher, dispatcher, dispatcher),
            appoxeeContainer = container,
        )
        sdks.add(sdk)
        return sdk
    }

    private fun assertTargets(clazz: Class<*>) {
        assertThat(sent.isCaptured).isTrue()
        assertThat(sent.captured.component?.className).isEqualTo(clazz.name)
        assertThat(sent.captured.component?.packageName).isEqualTo(realContext.packageName)
        assertThat(sent.captured.action).isEqualTo(LocalPushBroadcast.PUSH_RECEIVED)
        assertThat(sent.captured.hasExtra("mappPush")).isTrue()
    }

    private abstract class FirstReceiver : LocalPushBroadcast()
    private abstract class SecondReceiver : LocalPushBroadcast()
}
