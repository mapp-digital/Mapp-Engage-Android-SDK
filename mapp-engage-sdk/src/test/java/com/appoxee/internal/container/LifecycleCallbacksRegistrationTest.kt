package com.appoxee.internal.container

import android.app.Application
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertThrows
import org.junit.Test

class LifecycleCallbacksRegistrationTest {
    private val application = mockk<Application>()
    private val callbacks = mockk<Application.ActivityLifecycleCallbacks>()
    private val registration = LifecycleCallbacksRegistration()

    @Test
    fun `three initializations register the listener once`() {
        every { application.registerActivityLifecycleCallbacks(callbacks) } just runs

        repeat(3) { registration.registerOnce(application, callbacks) }

        verify(exactly = 1) { application.registerActivityLifecycleCallbacks(callbacks) }
    }

    @Test
    fun `failed platform registration can be retried`() {
        var attempts = 0
        every { application.registerActivityLifecycleCallbacks(callbacks) } answers {
            if (++attempts == 1) throw IllegalStateException("registration failed")
        }

        assertThrows(IllegalStateException::class.java) {
            registration.registerOnce(application, callbacks)
        }
        repeat(2) { registration.registerOnce(application, callbacks) }

        verify(exactly = 2) { application.registerActivityLifecycleCallbacks(callbacks) }
    }

    @Test
    fun `registration state is owned by each container rather than a global flag`() {
        every { application.registerActivityLifecycleCallbacks(callbacks) } just runs

        registration.registerOnce(application, callbacks)
        LifecycleCallbacksRegistration().registerOnce(application, callbacks)

        verify(exactly = 2) { application.registerActivityLifecycleCallbacks(callbacks) }
    }
}
