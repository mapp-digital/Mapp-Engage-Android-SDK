package com.appoxee.internal.push.style

import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.appoxee.internal.ui.push.model.PushData
import com.appoxee.internal.ui.push.style.NotificationImageLoader
import com.appoxee.internal.ui.push.style.NotificationImageStyle
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Test

class NotificationImageStyleTest {
    private val mediaUrl = "https://images.example.com/image.png"
    private val pushData = PushData(id = 1, title = "Title", bigText = "Body", iosApxMedia = mediaUrl)
    private val imageLoader = mockk<NotificationImageLoader>()

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `loaded image produces a picture notification after suspension`() = runTest {
        val bitmap = mockk<Bitmap>()
        coEvery { imageLoader.load(mediaUrl) } coAnswers {
            yield()
            bitmap
        }

        val style = NotificationImageStyle(pushData, imageLoader).getStyle()

        assertThat(style).isInstanceOf(NotificationCompat.BigPictureStyle::class.java)
    }

    @Test
    fun `unavailable image falls back to a text notification after suspension`() = runTest {
        coEvery { imageLoader.load(mediaUrl) } coAnswers {
            yield()
            null
        }

        val style = NotificationImageStyle(pushData, imageLoader).getStyle()

        assertThat(style).isInstanceOf(NotificationCompat.BigTextStyle::class.java)
    }

    @Test
    fun `image loading cancellation propagates without a fallback notification`() = runTest {
        val cancellation = CancellationException("Notification cancelled")
        coEvery { imageLoader.load(mediaUrl) } coAnswers {
            yield()
            throw cancellation
        }

        try {
            NotificationImageStyle(pushData, imageLoader).getStyle()
            throw AssertionError("Expected image loading cancellation to propagate")
        } catch (actual: CancellationException) {
            assertThat(actual).isSameInstanceAs(cancellation)
        }
    }
}
