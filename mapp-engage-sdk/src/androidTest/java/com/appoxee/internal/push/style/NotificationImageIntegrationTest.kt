package com.appoxee.internal.push.style

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appoxee.internal.ui.push.model.PushData
import com.appoxee.internal.ui.push.style.NotificationImageLoader
import com.appoxee.internal.ui.push.style.NotificationImageStyle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/** Real TLS, sockets, Android decoding and notification construction; no SDK behavior is mocked. */
@RunWith(AndroidJUnit4::class)
class NotificationImageIntegrationTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var loader: NotificationImageLoader

    @Before
    fun setUp() {
        val certificate = HeldCertificate.Builder()
            .addSubjectAlternativeName("localhost")
            .addSubjectAlternativeName("127.0.0.1")
            .addSubjectAlternativeName("::1")
            .build()
        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(certificate)
            .build()
        val clientCertificates = HandshakeCertificates.Builder()
            .addTrustedCertificate(certificate.certificate)
            .build()
        server = MockWebServer().apply {
            useHttps(serverCertificates.sslSocketFactory(), false)
            start()
        }
        // Trust only this test server. Platform hostname verification stays enabled.
        client = OkHttpClient.Builder()
            .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .build()
        loader = NotificationImageLoader(client)
    }

    @After
    fun tearDown() {
        if (::client.isInitialized) client.dispatcher.cancelAll()
        if (::server.isInitialized) server.shutdown()
        if (::client.isInitialized) {
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
        }
    }

    @Test
    fun downloadsHttpsImageAndDecodesBoundedBitmapWithCorrectPixels() = runTest {
        server.enqueue(imageResponse(png(width = 2049, height = 1024)))

        val bitmap = checkNotNull(loader.load(server.url("/large.png").toString()))
        try {
            assertThat(bitmap.width).isIn(1..1024)
            assertThat(bitmap.height).isIn(1..1024)
            assertThat(bitmap.getPixel(0, 0)).isEqualTo(IMAGE_COLOR)
            assertThat(server.requestCount).isEqualTo(1)
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun followsRelativeHttpsRedirectAndDecodesImage() = runTest {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/image.png"))
        server.enqueue(imageResponse(png()))

        val bitmap = checkNotNull(loader.load(server.url("/redirect").toString()))
        try {
            assertThat(bitmap.getPixel(0, 0)).isEqualTo(IMAGE_COLOR)
            assertThat(server.takeRequest(5, TimeUnit.SECONDS)?.path).isEqualTo("/redirect")
            assertThat(server.takeRequest(5, TimeUnit.SECONDS)?.path).isEqualTo("/image.png")
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun rejectsRedirectToHttpWithoutRequestingTheDestination() = runTest {
        val destination = MockWebServer()
        destination.start()
        try {
            destination.enqueue(imageResponse(png()))
            server.enqueue(MockResponse().setResponseCode(302)
                .setHeader("Location", destination.url("/image.png")))

            assertThat(loader.load(server.url("/redirect").toString())).isNull()

            assertThat(server.requestCount).isEqualTo(1)
            assertThat(destination.requestCount).isEqualTo(0)
        } finally {
            destination.shutdown()
        }
    }

    @Test
    fun rejectsOversizedFixedAndChunkedResponsesThenCanLoadAnotherImage() = runTest {
        val oversized = ByteArray(5 * 1024 * 1024 + 1)
        server.enqueue(MockResponse().setBody(Buffer().write(oversized)))
        server.enqueue(MockResponse().setChunkedBody(Buffer().write(oversized), 8192))
        server.enqueue(imageResponse(png()))

        assertThat(loader.load(server.url("/oversized-fixed.png").toString())).isNull()
        assertThat(loader.load(server.url("/oversized-chunked.png").toString())).isNull()
        val bitmap = checkNotNull(loader.load(server.url("/valid.png").toString()))
        try {
            assertThat(bitmap.getPixel(0, 0)).isEqualTo(IMAGE_COLOR)
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun unavailableImageProducesTextNotificationWithOriginalContent() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        val push = PushData(id = 1, title = "Title", bigText = "Body",
            iosApxMedia = server.url("/missing.png").toString())

        val style = NotificationImageStyle(push, loader).getStyle()
        val notification = notification(push, style)

        assertThat(style).isInstanceOf(NotificationCompat.BigTextStyle::class.java)
        assertThat(notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()).isEqualTo("Title")
        assertThat(notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()).isEqualTo("Body")
    }

    @Test
    fun validImageProducesPictureNotification() = runTest {
        server.enqueue(imageResponse(png()))
        val push = PushData(id = 1, title = "Title", bigText = "Body",
            iosApxMedia = server.url("/image.png").toString())

        val style = NotificationImageStyle(push, loader).getStyle()
        val notification = notification(push, style)

        assertThat(style).isInstanceOf(NotificationCompat.BigPictureStyle::class.java)
        assertThat(notification.extras.getString(Notification.EXTRA_TEMPLATE))
            .isEqualTo(Notification.BigPictureStyle::class.java.name)
    }

    @Test
    fun invalidImageBytesFallBackToTextNotification() = runTest {
        server.enqueue(imageResponse("not a PNG image".toByteArray()))
        val push = PushData(id = 1, iosApxMedia = server.url("/invalid.png").toString())

        assertThat(NotificationImageStyle(push, loader).getStyle())
            .isInstanceOf(NotificationCompat.BigTextStyle::class.java)
    }

    @Test
    fun defaultLoaderRejectsUntrustedServerCertificate() = runTest {
        server.enqueue(imageResponse(png()))

        // The production client must not inherit the integration client's test-only trust.
        assertThat(NotificationImageLoader.default.load(server.url("/image.png").toString())).isNull()
    }

    @Test
    fun queueWaitSharesTheLoadingDeadlineWithRealStalledConnections() = runTest {
        // Keep transport timeouts above the SDK's 20-second budget so this
        // exercises the loader deadline, including its semaphore queue.
        client = client.newBuilder()
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
        loader = NotificationImageLoader(client)
        repeat(3) { server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)) }
        val first = async { loader.load(server.url("/first.png").toString()) }
        val second = async { loader.load(server.url("/second.png").toString()) }
        withContext(Dispatchers.IO) {
            checkNotNull(server.takeRequest(5, TimeUnit.SECONDS))
            checkNotNull(server.takeRequest(5, TimeUnit.SECONDS))
        }
        assertThat(server.requestCount).isEqualTo(2)
        val startedAt = SystemClock.elapsedRealtime()

        assertThat(loader.load(server.url("/queued.png").toString())).isNull()

        // The former implementation waited for a slot and then started another
        // 20-second timeout, taking about 40 seconds under these conditions.
        assertThat(SystemClock.elapsedRealtime() - startedAt).isLessThan(30_000L)
        assertThat(first.await()).isNull()
        assertThat(second.await()).isNull()
    }

    @Test
    fun cancellingLoadCancelsTheActualHttpCallAndReleasesItsSlot() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val pending = async { loader.load(server.url("/stalled.png").toString()) }
        val request = withContext(Dispatchers.IO) { server.takeRequest(5, TimeUnit.SECONDS) }
        assertThat(request?.path).isEqualTo("/stalled.png")
        val runningCalls = client.dispatcher.runningCalls()
        assertThat(runningCalls).hasSize(1)

        pending.cancelAndJoin()

        assertThat(pending.isCancelled).isTrue()
        assertThat(runningCalls.single().isCanceled()).isTrue()
        server.enqueue(imageResponse(png()))
        val bitmap = checkNotNull(loader.load(server.url("/next.png").toString()))
        bitmap.recycle()
    }

    private fun notification(push: PushData, style: NotificationCompat.Style): Notification {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return NotificationCompat.Builder(context, "image-integration-test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(push.title)
            .setContentText(push.bigText)
            .setStyle(style)
            .build()
    }

    private fun imageResponse(bytes: ByteArray): MockResponse = MockResponse()
        .setHeader("Content-Type", "image/png")
        .setBody(Buffer().write(bytes))

    private fun png(width: Int = 32, height: Int = 16): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(IMAGE_COLOR)
            return ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private companion object {
        val IMAGE_COLOR: Int = Color.rgb(24, 80, 180)
    }
}
