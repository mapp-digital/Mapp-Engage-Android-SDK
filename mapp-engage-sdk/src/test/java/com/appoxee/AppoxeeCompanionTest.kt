package com.appoxee

import android.app.Application
import android.content.Context
import android.os.Looper
import com.appoxee.internal.AppoxeeImpl
import com.appoxee.internal.SdkInitialization
import com.appoxee.internal.util.Logger
import com.appoxee.shared.AppoxeeOptions
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertThrows
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class AppoxeeCompanionTest {
    private var creations = 0

    private fun options(key: String = "key") =
        AppoxeeOptions(AppoxeeOptions.Server.TEST, key, "app", "tenant")

    private fun application(): Application = mockk<Application>(relaxed = true).also { app ->
        every { app.applicationContext } returns app
    }

    private fun sdk(app: Application, options: AppoxeeOptions?): AppoxeeImpl {
        var initialization = SdkInitialization(options)
        return mockk<AppoxeeImpl>(relaxed = true).also { instance ->
            every { instance.application } returns app
            every { instance.initialization } answers { initialization }
            every { instance.reengage(any()) } answers {
                val requested = firstArg<AppoxeeOptions?>()
                if (!initialization.canReuse(requested)) {
                    initialization = SdkInitialization(requested ?: initialization.configuration)
                }
            }
            every { instance.isReady() } answers { initialization.status == SdkInitialization.Status.READY }
        }
    }

    @Before
    fun setUp() {
        mockkStatic(Looper::class)
        mockkObject(Logger.Companion)
        val mainLooper = mockk<Looper>()
        every { Looper.getMainLooper() } returns mainLooper
        every { mainLooper.thread } answers { Thread.currentThread() }
        every { Logger.d(any(), any()) } returns Unit
        Appoxee.resetForTests()
        creations = 0
        Appoxee.instanceFactory = { app, options, _ ->
            creations++
            sdk(app, options)
        }
    }

    @After
    fun tearDown() {
        Appoxee.resetForTests()
        unmockkAll()
    }

    @Test
    fun `engage does not log raw options`() {
        val context = mockk<Context>(relaxed = true)
        val application = mockk<Application>(relaxed = true)
        every { context.applicationContext } returns application

        val logMessage = slot<String>()
        every { Logger.d(any(), capture(logMessage)) } returns Unit

        val options = AppoxeeOptions(
            server = AppoxeeOptions.Server.TEST,
            sdkKey = "sensitive-sdk-key",
            appId = "sensitive-app-id",
            tenantId = "tenant"
        )

        Appoxee.engage(context, options)

        assertThat(logMessage.captured).contains("optionsProvided=true")
        assertThat(logMessage.captured).doesNotContain("sensitive-sdk-key")
        assertThat(logMessage.captured).doesNotContain("sensitive-app-id")
    }

    @Test
    fun `concurrent engage and instance access is stable`() {
        val context = mockk<Context>(relaxed = true)
        val application = mockk<Application>(relaxed = true)
        every { context.applicationContext } returns application

        val errors = CopyOnWriteArrayList<Throwable>()
        val threadCount = 8
        val iterations = 25
        val done = CountDownLatch(threadCount)

        repeat(threadCount) {
            thread(start = true) {
                try {
                    repeat(iterations) {
                        Appoxee.engage(context, null)
                        Appoxee.instance()
                    }
                } catch (t: Throwable) {
                    errors.add(t)
                } finally {
                    done.countDown()
                }
            }
        }

        done.await()

        assertThat(errors).isEmpty()
        assertThat(Appoxee.instance()).isNotNull()
        assertThat(creations).isEqualTo(1)
    }

    @Test
    fun `repeated engage reuses the same initializing instance`() {
        val app = application()
        Appoxee.engage(app, options())
        val first = Appoxee.instance()
        repeat(3) { Appoxee.engage(app, options()) }
        Appoxee.engage(app)
        assertThat(Appoxee.instance()).isSameInstanceAs(first)
        assertThat(creations).isEqualTo(1)
    }

    @Test
    fun `repeated engage reuses a valid initialized instance`() {
        val app = application()
        Appoxee.engage(app, options())
        val first = Appoxee.instance() as AppoxeeImpl
        first.initialization.complete(ready = true)
        repeat(3) { Appoxee.engage(app, options()) }
        Appoxee.engage(app)
        assertThat(Appoxee.instance()).isSameInstanceAs(first)
        assertThat(creations).isEqualTo(1)
    }

    @Test
    fun `failed initialization retries with intended options even without explicit options`() {
        val app = application()
        val original = options()
        original.notificationMode = com.appoxee.shared.NotificationMode.BACKGROUND_AND_FOREGROUND
        Appoxee.engage(app, original)
        val first = Appoxee.instance() as AppoxeeImpl
        original.notificationMode = com.appoxee.shared.NotificationMode.SILENT_ONLY
        first.initialization.complete(ready = false)
        Appoxee.engage(app)
        val retried = Appoxee.instance() as AppoxeeImpl
        assertThat(retried).isSameInstanceAs(first)
        assertThat(retried.initialization.configuration!!.sameEngagement(options().apply {
            notificationMode = com.appoxee.shared.NotificationMode.BACKGROUND_AND_FOREGROUND
        })).isTrue()
        assertThat(creations).isEqualTo(1)
    }

    @Test
    fun `changed configuration is applied and subsequent calls reuse it`() {
        val app = application()
        Appoxee.engage(app, options("a"))
        val first = Appoxee.instance()
        Appoxee.engage(app, options("b"))
        val second = Appoxee.instance()
        assertThat(second).isSameInstanceAs(first)
        assertThat((second as AppoxeeImpl).initialization.configuration!!.sdkKey).isEqualTo("b")
        Appoxee.engage(app, options("b"))
        Appoxee.engage(app)
        assertThat(Appoxee.instance()).isSameInstanceAs(second)
        assertThat(creations).isEqualTo(1)
    }

    @Test
    fun `notification mode and logging changes cannot be hidden by public equals`() {
        val app = application()
        val requested = options()
        Appoxee.engage(app, requested)
        requested.notificationMode = com.appoxee.shared.NotificationMode.SILENT_ONLY
        Appoxee.engage(app, requested)
        assertThat((Appoxee.instance() as AppoxeeImpl).initialization.configuration!!.notificationMode)
            .isEqualTo(com.appoxee.shared.NotificationMode.SILENT_ONLY)
        requested.logType = AppoxeeOptions.LogLevel.RELEASE
        Appoxee.engage(app, requested)
        assertThat((Appoxee.instance() as AppoxeeImpl).initialization.configuration!!.logType)
            .isEqualTo(AppoxeeOptions.LogLevel.RELEASE)
        assertThat(creations).isEqualTo(1)
    }

    @Test
    fun `explicit options after unknown background bootstrap must be applied`() {
        val app = application()
        Appoxee.engage(app)
        Appoxee.engage(app, options())
        assertThat(creations).isEqualTo(1)
    }

    @Test
    fun `equivalent options after loading saved configuration reuse the existing SDK`() {
        val app = application()
        Appoxee.engage(app)
        (Appoxee.instance() as AppoxeeImpl).initialization.resolveSavedConfiguration(options())
        Appoxee.engage(app, options())
        assertThat(creations).isEqualTo(1)
    }

    @Test
    fun `reconfiguration never invokes the instance factory again`() {
        val app = application()
        Appoxee.engage(app, options("a"))
        val first = Appoxee.instance()
        Appoxee.instanceFactory = { _, _, _ -> throw IllegalStateException("construction failed") }
        Appoxee.engage(app, options("b"))
        Appoxee.engage(app, options("a"))
        assertThat(Appoxee.instance()).isSameInstanceAs(first)
        assertThat(creations).isEqualTo(1)
    }

    @Test
    fun `another Application cannot replace the process SDK`() {
        val app = application()
        Appoxee.engage(app, options())
        val first = Appoxee.instance()
        assertThrows(IllegalArgumentException::class.java) {
            Appoxee.engage(application(), options())
        }
        assertThat(Appoxee.instance()).isSameInstanceAs(first)
        assertThat(creations).isEqualTo(1)
    }

    @Test
    fun `reuse still requires the main thread`() {
        val app = application()
        Appoxee.engage(app, options())
        val mainLooper = mockk<Looper>()
        every { Looper.getMainLooper() } returns mainLooper
        every { mainLooper.thread } returns Thread()
        assertThrows(IllegalAccessException::class.java) { Appoxee.engage(app, options()) }
        assertThat(creations).isEqualTo(1)
    }

}
