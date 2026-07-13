package com.appoxee.internal.ui.inapp

import android.view.View
import android.widget.ImageButton
import com.appoxee.internal.model.response.inapp.Behaviour
import com.appoxee.internal.model.response.inapp.Message
import com.appoxee.internal.util.DispatchersProvider
import com.appoxee.sdk.R
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TemplateTest {

    @Test
    fun `zero display seconds does not dismiss immediately`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val template = testTemplate(this, dispatcher)
        val view = viewWithoutCloseButton()
        var dismissed = false

        template.createView(message(displaySeconds = 0), view) {
            dismissed = true
        }
        advanceUntilIdle()

        assertThat(dismissed).isFalse()
    }

    @Test
    fun `positive display seconds dismisses after expiration`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val template = testTemplate(this, dispatcher)
        val view = viewWithoutCloseButton()
        var dismissed = false

        template.createView(message(displaySeconds = 1), view) {
            dismissed = true
        }
        advanceTimeBy(999)
        runCurrent()
        assertThat(dismissed).isFalse()

        advanceTimeBy(1)
        runCurrent()

        assertThat(dismissed).isTrue()
    }

    private fun testTemplate(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher
    ): TestTemplate {
        return TestTemplate(
            scope = scope,
            dispatchersProvider = object : DispatchersProvider {
                override val ioDispatcher: CoroutineDispatcher = dispatcher
                override val mainDispatcher: CoroutineDispatcher = dispatcher
                override val defaultDispatcher: CoroutineDispatcher = dispatcher
            }
        )
    }

    private fun message(displaySeconds: Int): Message {
        return mockk(relaxed = true) {
            every { behaviour } returns Behaviour(delaySeconds = 0, displaySeconds = displaySeconds)
        }
    }

    private fun viewWithoutCloseButton(): View {
        return mockk(relaxed = true) {
            every { findViewById<ImageButton>(R.id.ibClose) } returns null
        }
    }

    private class TestTemplate(
        scope: CoroutineScope,
        dispatchersProvider: DispatchersProvider
    ) : Template(
        inappActionHandler = mockk(relaxed = true),
        scope = scope,
        dispatchersProvider = dispatchersProvider
    ) {
        fun createView(message: Message, view: View, onDismiss: (() -> Unit)?) {
            onViewCreated(message, view, onDismiss)
        }

        override fun show() = Unit
    }
}
