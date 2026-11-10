package com.appoxee.internal.container

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.test.platform.app.InstrumentationRegistry
import com.appoxee.Appoxee
import com.appoxee.internal.AppoxeeImpl
import com.appoxee.internal.model.request.RegisterDevice
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.TestDispatchersProvider
import com.appoxee.internal.SdkInitialization
import com.appoxee.internal.stats.StatsClient
import com.appoxee.internal.storage.InMemoryStorageImpl
import com.appoxee.internal.ui.ActivityLifecycleHandler
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.LocalPushBroadcast
import com.appoxee.shared.MappResult
import com.appoxee.shared.NotificationMode
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Real SDK construction and session handler; only storage/network and the platform boundary are faked. */
@OptIn(ExperimentalCoroutinesApi::class)
class LifecycleRegistrationIntegrationTest {
    private val realContext = ApplicationProvider.getApplicationContext<Context>()
    private val application = mockk<Application>(relaxed = true)
    private val container = mockk<AppoxeeContainer>(relaxed = true)
    private val storage = spyk(InMemoryStorageImpl())
    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = TestDispatchersProvider(dispatcher, dispatcher, dispatcher)
    private val registration = LifecycleCallbacksRegistration()
    private val callbacks = mutableListOf<Application.ActivityLifecycleCallbacks>()
    private val sdks = mutableListOf<AppoxeeImpl>()
    private val stats = mockk<StatsClient>(relaxed = true)
    private val reports = AtomicInteger()
    private val reported = CountDownLatch(1)
    private val options = AppoxeeOptions(AppoxeeOptions.Server.TEST_55, "key", "app", "tenant")
    private val bootstrap = CompletableDeferred<AppoxeeOptions?>()
    private val component = ComponentName("test.app", "test.app.LaunchActivity")
    private lateinit var handler: ActivityLifecycleHandler
    private var failRegistration = false
    private var registeredReceiver: Class<*>? = null

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(ProcessLifecycleOwner.Companion)
        every { ProcessLifecycleOwner.get() } returns mockk(relaxed = true)
        Appoxee.resetForTests()
        every { application.applicationContext } returns application
        every { application.applicationInfo } returns realContext.applicationInfo
        every { application.registerActivityLifecycleCallbacks(any()) } answers {
            if (failRegistration) {
                failRegistration = false
                throw IllegalStateException("platform registration failed")
            }
            callbacks.add(firstArg())
            Unit
        }
        val context = mockk<Context>(relaxed = true)
        val packageManager = mockk<PackageManager>()
        every { context.packageName } returns "test.app"
        every { context.packageManager } returns packageManager
        every { packageManager.getLaunchIntentForPackage("test.app") } returns Intent().setComponent(component)
        handler = ActivityLifecycleHandler(context, stats, dispatchers)
        every { container.activityLifecycleHandler } returns handler
        every { container.lifecycleCallbacksRegistration } returns registration
        every { container.initializationMutex } returns Mutex()
        every { container.storage } returns storage
        every { container.pushBroadcastMutex } returns Mutex()
        every { container.localPushBroadcast } answers { registeredReceiver }
        every { container.localPushBroadcast = any() } answers { registeredReceiver = firstArg() }
        // Keep every real SDK scope alive, but stop before registration/network work.
        coEvery { storage.getInitOptions() } coAnswers { bootstrap.await() }
        coEvery { stats.reportActivation(any()) } coAnswers {
            reports.incrementAndGet()
            reported.countDown()
        }
        Appoxee.instanceFactory = { app, requested, _ ->
            AppoxeeImpl(app, requested, dispatchers, appoxeeContainer = container).also(sdks::add)
        }
    }

    @After
    fun tearDown() {
        sdks.forEach { it.internalScope.cancel() }
        dispatcher.scheduler.runCurrent()
        Appoxee.resetForTests()
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun threeEngagementsIncludingConfigurationChangeProduceOneSessionReport() {
        onMain {
            Appoxee.engage(application, options)
            Appoxee.engage(application, options)
            Appoxee.engage(application, AppoxeeOptions(options.server, "other-key", "app", "tenant"))
        }

        assertThat(sdks).hasSize(1)
        dispatcher.scheduler.runCurrent()
        assertThat(callbacks).containsExactly(handler)
        endOneSession()
    }

    @Test
    fun platformFailureDoesNotPreventSuccessfulRetryOrDuplicateItsSessionReport() {
        failRegistration = true
        onMain {
            try {
                Appoxee.engage(application, options)
                throw AssertionError("Expected platform failure")
            } catch (_: IllegalStateException) {
                // The failed instance was never installed; the shared owner remains retryable.
            }
            Appoxee.engage(application, options)
            Appoxee.engage(application, options)
        }

        assertThat(sdks).hasSize(1)
        dispatcher.scheduler.runCurrent()
        assertThat(callbacks).containsExactly(handler)
        endOneSession()
    }

    @Test
    fun threeSameConfigurationEngagementsCreateOneSdkAndOneSessionReport() {
        onMain {
            repeat(3) { Appoxee.engage(application, options) }
            Appoxee.engage(application)
        }
        dispatcher.scheduler.runCurrent()
        assertThat(sdks).hasSize(1)
        assertThat(sdks.single().initialization.status).isEqualTo(SdkInitialization.Status.INITIALIZING)
        assertThat(callbacks).containsExactly(handler)
        endOneSession()
    }

    @Test
    fun bootstrapFailureBeforeAnyObserverIsRetryableAndRetainsExplicitConfiguration() {
        var reads = 0
        coEvery { storage.getInitOptions() } coAnswers {
            if (++reads == 1) throw IllegalStateException("storage unavailable")
            bootstrap.await()
        }
        onMain { Appoxee.engage(application, options) }
        dispatcher.scheduler.runCurrent()
        assertThat(sdks.single().initialization.status).isEqualTo(SdkInitialization.Status.FAILED)

        onMain { Appoxee.engage(application) }
        dispatcher.scheduler.runCurrent()
        assertThat(sdks).hasSize(1)
        assertThat(sdks.last().initialization.configuration!!.sameEngagement(options)).isTrue()
        assertThat(sdks.last().initialization.status).isEqualTo(SdkInitialization.Status.INITIALIZING)
        assertThat(callbacks).containsExactly(handler)
    }

    @Test
    fun rapidConfigurationChangesWaitForPreviousBootstrapToStopBeforeReadingStorage() {
        val oldReadStarted = CompletableDeferred<Unit>()
        val releaseOldRead = CompletableDeferred<Unit>()
        var reads = 0
        coEvery { storage.getInitOptions() } coAnswers {
            reads++
            if (reads == 1) {
                oldReadStarted.complete(Unit)
                // Represents an operation which does not stop immediately when cancelled.
                withContext(NonCancellable) { releaseOldRead.await() }
                null
            } else bootstrap.await()
        }
        onMain { Appoxee.engage(application, options) }
        dispatcher.scheduler.runCurrent()
        assertThat(oldReadStarted.isCompleted).isTrue()
        val firstJob = sdks.single().initializationJob

        try {
            onMain {
                Appoxee.engage(application, AppoxeeOptions(options.server, "new-key", "app", "tenant"))
                // Supersede the middle attempt before its coroutine even starts.
                Appoxee.engage(application, AppoxeeOptions(options.server, "newest-key", "app", "tenant"))
            }
            dispatcher.scheduler.runCurrent()
            assertThat(sdks).hasSize(1)
            assertThat(reads).isEqualTo(1)
            assertThat(firstJob.isCancelled).isTrue()

            releaseOldRead.complete(Unit)
            dispatcher.scheduler.runCurrent()
            assertThat(firstJob.isCompleted).isTrue()
            assertThat(reads).isEqualTo(2)
            assertThat(sdks.last().initialization.configuration!!.sdkKey).isEqualTo("newest-key")
            assertThat(sdks.last().initialization.status).isEqualTo(SdkInitialization.Status.INITIALIZING)
            coVerify(exactly = 0) { storage.saveInitOptions(any()) }
        } finally {
            releaseOldRead.complete(Unit)
            dispatcher.scheduler.runCurrent()
        }
    }

    @Test
    fun readySdkReusesItsInstanceAndJobWithoutRepeatingBootstrap() {
        allowBootstrapToBecomeReady()
        onMain { Appoxee.engage(application, options) }
        dispatcher.scheduler.runCurrent()
        val sdk = sdks.single()
        val job = sdk.initializationJob
        assertThat(sdk.isReady()).isTrue()
        assertThat(sdk.initialization.status).isEqualTo(SdkInitialization.Status.READY)

        onMain {
            repeat(3) { Appoxee.engage(application, options) }
            Appoxee.engage(application)
        }
        dispatcher.scheduler.runCurrent()
        assertThat(Appoxee.instance()).isSameInstanceAs(sdk)
        assertThat(sdk.initializationJob).isSameInstanceAs(job)
        assertThat(sdks).hasSize(1)
        coVerify(exactly = 1) { storage.getInitOptions() }
    }

    @Test
    fun reconfigurationPreservesInstanceScopeObserversAndReceiverAndBecomesReadyAgain() {
        allowBootstrapToBecomeReady()
        onMain { Appoxee.engage(application, options) }
        val sdk = sdks.single()
        val scope = sdk.internalScope
        val readiness = mutableListOf<Boolean>()
        sdk.subscribe(AppoxeeObserver { ready, _ -> readiness.add(ready) })
        sdk.setPushBroadcast(TestReceiver::class.java)
        dispatcher.scheduler.runCurrent()
        assertThat(sdk.isReady()).isTrue()
        assertThat(readiness).containsExactly(true)
        val firstAttempt = sdk.initialization
        readiness.clear()

        val next = AppoxeeOptions(options.server, "new-key", "new-app", "new-tenant")
        onMain {
            Appoxee.engage(application, next)
            assertThat(Appoxee.instance()).isSameInstanceAs(sdk)
            assertThat(sdk.isReady()).isFalse()
        }
        dispatcher.scheduler.runCurrent()

        assertThat(sdks).hasSize(1)
        assertThat(sdk.internalScope).isSameInstanceAs(scope)
        assertThat(scope.isActive).isTrue()
        assertThat(sdk.initialization).isNotSameInstanceAs(firstAttempt)
        assertThat(sdk.initialization.configuration!!.sameEngagement(next)).isTrue()
        assertThat(sdk.initialization.status).isEqualTo(SdkInitialization.Status.READY)
        assertThat(sdk.isReady()).isTrue()
        assertThat(readiness).containsExactly(true)
        assertThat(registeredReceiver).isEqualTo(TestReceiver::class.java)
        assertThat(runBlocking { storage.getBroadcastClass() }).isEqualTo(TestReceiver::class.java)
        assertThat(runBlocking { storage.getInitOptions() }!!.sameEngagement(next)).isTrue()
        assertThat(callbacks).containsExactly(handler)
        endOneSession()
    }

    @Test
    fun retryKeepsItsObserverAndScopeAndRecoversOnTheSameSdk() {
        coEvery { storage.getInitOptions() } throws IllegalStateException("storage unavailable")
        onMain { Appoxee.engage(application, options) }
        val sdk = sdks.single()
        val scope = sdk.internalScope
        val readiness = mutableListOf<Boolean>()
        sdk.subscribe(AppoxeeObserver { ready, _ -> readiness.add(ready) })
        dispatcher.scheduler.runCurrent()
        assertThat(sdk.initialization.status).isEqualTo(SdkInitialization.Status.FAILED)
        assertThat(readiness).containsExactly(false)
        val failedAttempt = sdk.initialization

        allowBootstrapToBecomeReady()
        onMain { Appoxee.engage(application) }
        dispatcher.scheduler.runCurrent()
        assertThat(Appoxee.instance()).isSameInstanceAs(sdk)
        assertThat(sdks).hasSize(1)
        assertThat(sdk.internalScope).isSameInstanceAs(scope)
        assertThat(scope.isActive).isTrue()
        assertThat(sdk.initialization).isNotSameInstanceAs(failedAttempt)
        assertThat(sdk.isReady()).isTrue()
        assertThat(readiness).containsExactly(false, true).inOrder()
    }

    @Test
    fun retryAndSettingsChangesRetainQueuedPushesButChangedCredentialsDiscardThem() {
        coEvery { storage.getInitOptions() } throws IllegalStateException("failed")
        onMain { Appoxee.engage(application, options) }
        dispatcher.scheduler.runCurrent()
        val sdk = sdks.single()
        val message = mockk<RemoteMessage>(relaxed = true)
        sdk.addToQueue(message)
        coEvery { storage.getInitOptions() } coAnswers { bootstrap.await() }

        onMain { Appoxee.engage(application) }
        dispatcher.scheduler.runCurrent()
        assertThat(sdk.pushQueue).containsExactly(message)
        val settings = options.engagementSnapshot().apply { notificationMode = NotificationMode.SILENT_ONLY }
        onMain { Appoxee.engage(application, settings) }
        dispatcher.scheduler.runCurrent()
        assertThat(sdk.pushQueue).containsExactly(message)

        onMain {
            Appoxee.engage(application, AppoxeeOptions(options.server, "new-key", "app", "tenant"))
        }
        dispatcher.scheduler.runCurrent()
        assertThat(sdk.pushQueue).isEmpty()
        assertThat(Appoxee.instance()).isSameInstanceAs(sdk)
        assertThat(sdks).hasSize(1)
    }

    @Test
    fun channelChangeWaitsForOldCallsAndRejectsUnusedCallsFromTheOldChannel() {
        var reads = 0
        coEvery { storage.getInitOptions() } coAnswers {
            reads++
            bootstrap.await()
        }
        onMain { Appoxee.engage(application, options) }
        dispatcher.scheduler.runCurrent()
        val sdk = sdks.single()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val results = mutableListOf<MappResult<String>>()
        var obsoleteCallRan = false
        val unused = sdk.buildHttpCall {
            obsoleteCallRan = true
            "obsolete"
        }
        sdk.buildHttpCall {
            started.complete(Unit)
            withContext(NonCancellable) { release.await() }
            "old result"
        }.enqueue { results.add(it) }
        dispatcher.scheduler.runCurrent()
        assertThat(started.isCompleted).isTrue()

        try {
            onMain {
                Appoxee.engage(application, AppoxeeOptions(options.server, "new-key", "app", "tenant"))
            }
            unused.enqueue { results.add(it) }
            dispatcher.scheduler.runCurrent()
            assertThat(reads).isEqualTo(1)
            assertThat(obsoleteCallRan).isFalse()

            release.complete(Unit)
            dispatcher.scheduler.runCurrent()
            assertThat(reads).isEqualTo(2)
            assertThat(results).hasSize(2)
            assertThat(results.all { !it.isSuccess() }).isTrue()
            assertThat(sdk.initialization.status).isEqualTo(SdkInitialization.Status.INITIALIZING)
            assertThat(sdks).hasSize(1)
        } finally {
            release.complete(Unit)
            dispatcher.scheduler.runCurrent()
        }
    }

    private abstract class TestReceiver : LocalPushBroadcast()

    private fun allowBootstrapToBecomeReady() {
        val device = DevicePayload(udidHashed = "test-device", pushToken = "token")
        val registration = RegisterDevice()
        runBlocking {
            storage.saveInitOptions(options)
            storage.saveDevicePayload(device)
            storage.saveRegistrationDevice(registration)
            storage.updateDeviceFetchTimestamp()
        }
        coEvery { storage.getInitOptions() } coAnswers { callOriginal() }
        coEvery { storage.isCacheValid() } returns true
        coEvery { container.deviceProvider.generateRegistrationDevice() } returns registration
        coEvery { container.migrationHelper.getRegistrationOptions() } returns null
        coEvery { container.migrationHelper.fetchRegistrationData() } returns null
        coEvery { container.appoxeeAdapter.register(any()) } returns null
        coEvery { container.appoxeeAdapter.getDevice() } returns device
        val messaging = mockk<FirebaseMessaging>()
        mockkStatic(FirebaseMessaging::class)
        every { FirebaseMessaging.getInstance() } returns messaging
        every { messaging.token } returns Tasks.forResult("token")
    }

    private fun endOneSession() {
        val activity = mockk<Activity>(relaxed = true)
        every { activity.componentName } returns component
        every { activity.intent } returns Intent()
        onMain {
            callbacks.forEach { it.onActivityStarted(activity) }
            callbacks.forEach { it.onActivityStopped(activity) }
        }
        assertThat(reported.await(2, TimeUnit.SECONDS)).isTrue()
        coVerify(exactly = 1, timeout = 1_000) { stats.reportActivation(any()) }
        assertThat(reports.get()).isEqualTo(1)
    }

    private fun onMain(action: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
    }
}
