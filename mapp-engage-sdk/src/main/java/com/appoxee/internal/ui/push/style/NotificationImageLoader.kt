package com.appoxee.internal.ui.push.style

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.appoxee.internal.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Bounded, cancellable notification thumbnail loading; never persists campaign media. */
internal class NotificationImageLoader(
    private val callFactory: Call.Factory = httpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun load(path: String?): Bitmap? = withContext(ioDispatcher) {
        try {
            val initialUrl = path?.toHttpUrlOrNull()?.takeIf { it.isSafeImageUrl() }
                ?: return@withContext null
            imageSlots.withPermit {
                // The budget covers all redirects and body reads, including a slow trickle.
                withTimeoutOrNull(TOTAL_TIMEOUT_MS) {
                    val bytes = download(initialUrl)
                    currentCoroutineContext().ensureActive()
                    decode(bytes)
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Logger.w(TAG, "Notification image failed", error,
                releaseMessage = "Notification image unavailable; using text notification")
            null
        }
    }

    private suspend fun download(initialUrl: HttpUrl): ByteArray {
        var url = initialUrl
        repeat(MAX_REDIRECTS + 1) { redirects ->
            when (val result = fetch(url)) {
                is DownloadResult.Image -> return result.bytes
                is DownloadResult.Redirect -> {
                    if (redirects == MAX_REDIRECTS || !result.url.isSafeImageUrl()) {
                        throw IOException("Unsafe or excessive image redirects")
                    }
                    url = result.url
                }
            }
        }
        throw IOException("Image redirect limit exceeded")
    }

    private suspend fun fetch(url: HttpUrl): DownloadResult = suspendCancellableCoroutine { continuation ->
        val call = callFactory.newCall(Request.Builder().url(url).build())
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val result = response.use {
                        if (!continuation.isActive) return
                        if (it.code in REDIRECT_CODES) {
                            val destination = it.header("Location")?.let(url::resolve)
                                ?: throw IOException("Image redirect has no valid destination")
                            DownloadResult.Redirect(destination)
                        } else {
                            if (!it.isSuccessful) throw IOException("Image request failed")
                            val body = it.body ?: throw IOException("Image response is empty")
                            if (body.contentLength() > MAX_DOWNLOAD_BYTES) {
                                throw IOException("Image exceeds download limit")
                            }
                            val output = ByteArrayOutputStream()
                            body.byteStream().use { input ->
                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                while (true) {
                                    if (!continuation.isActive) return
                                    val count = input.read(buffer)
                                    if (count == -1) break
                                    if (output.size().toLong() + count > MAX_DOWNLOAD_BYTES) {
                                        throw IOException("Image exceeds download limit")
                                    }
                                    output.write(buffer, 0, count)
                                }
                            }
                            DownloadResult.Image(output.toByteArray())
                        }
                    }
                    continuation.resume(result)
                } catch (error: Exception) {
                    continuation.resumeWithException(error)
                }
            }
        })
    }

    private fun decode(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val sampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun HttpUrl.isSafeImageUrl(): Boolean =
        isHttps && username.isEmpty() && password.isEmpty()

    private sealed interface DownloadResult {
        class Image(val bytes: ByteArray) : DownloadResult
        class Redirect(val url: HttpUrl) : DownloadResult
    }

    internal companion object {
        private const val TAG = "NotificationImageLoader"
        private const val MAX_DOWNLOAD_BYTES = 5L * 1024 * 1024
        private const val MAX_SOURCE_PIXELS = 40_000_000L
        private const val MAX_SOURCE_DIMENSION = 32_768
        private const val MAX_DECODED_DIMENSION = 1024
        private const val MAX_REDIRECTS = 3
        private const val TOTAL_TIMEOUT_MS = 20_000L
        private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
        private val imageSlots = Semaphore(2)
        private val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .callTimeout(10, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(false)
                .build()
        }

        val default: NotificationImageLoader by lazy { NotificationImageLoader() }

        internal fun sampleSize(width: Int, height: Int): Int {
            if (width !in 1..MAX_SOURCE_DIMENSION || height !in 1..MAX_SOURCE_DIMENSION ||
                width.toLong() * height > MAX_SOURCE_PIXELS) {
                throw IOException("Invalid or excessive image dimensions")
            }
            var sample = 1
            while ((width + sample - 1) / sample > MAX_DECODED_DIMENSION ||
                (height + sample - 1) / sample > MAX_DECODED_DIMENSION) {
                sample *= 2
            }
            return sample
        }
    }
}
