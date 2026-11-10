package com.appoxee.internal.network

import TestDispatchersProvider
import android.util.Log
import com.appoxee.internal.network.exceptions.CallConsumedException
import com.appoxee.shared.MappCallback
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class HttpCallTest {
    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `execute returns success result`() {
        val dispatcher = StandardTestDispatcher()
        val dispatchersProvider = TestDispatchersProvider(dispatcher)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val sut = HttpCall(scope, call = { "ok" }, dispatchersProvider = dispatchersProvider)

        val result = sut.execute()

        assertThat(result.isSuccess()).isTrue()
        assertThat(result.getData()).isEqualTo("ok")
    }

    @Test
    fun `asSuspend returns error result when call throws`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dispatchersProvider = TestDispatchersProvider(dispatcher)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val expected = IllegalStateException("boom")
        val sut = HttpCall<String>(
            scope = scope,
            call = { throw expected },
            dispatchersProvider = dispatchersProvider
        )

        val result = sut.asSuspend()

        assertThat(result.isSuccess()).isFalse()
        assertThat(result.getError()).isEqualTo(expected)
    }

    @Test
    fun `enqueue delivers consumed-call error on second consumption`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dispatchersProvider = TestDispatchersProvider(dispatcher)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val sut = HttpCall(scope, call = { "ok" }, dispatchersProvider = dispatchersProvider)
        val callback = mockk<MappCallback<String>>(relaxed = true)

        val first = sut.asSuspend()
        sut.enqueue(callback)
        testScheduler.advanceUntilIdle()

        assertThat(first.isSuccess()).isTrue()
        verify {
            callback.onResult(match {
                !it.isSuccess() && it.getError() is CallConsumedException
            })
        }
    }

    @Test(expected = CallConsumedException::class)
    fun `execute throws when consumed`() {
        val dispatcher = StandardTestDispatcher()
        val dispatchersProvider = TestDispatchersProvider(dispatcher)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val sut = HttpCall(scope, call = { "ok" }, dispatchersProvider = dispatchersProvider)
        sut.execute()
        sut.execute()
    }

    @Test
    fun `enqueue rejects a response invalidated between IO completion and main delivery`() = runTest {
        val io = StandardTestDispatcher(testScheduler, "IO")
        val main = StandardTestDispatcher(testScheduler, "Main")
        val dispatchers = object : com.appoxee.internal.util.DispatchersProvider {
            override val ioDispatcher = io
            override val mainDispatcher = main
            override val defaultDispatcher = io
        }
        val scope = CoroutineScope(SupervisorJob() + main)
        var current = true
        var executed = false
        val callback = mockk<MappCallback<String>>(relaxed = true)
        val sut = HttpCall(
            scope = scope,
            call = {
                executed = true
                // Runs before the completed response resumes on main.
                scope.launch(io) { current = false }
                "old channel response"
            },
            dispatchersProvider = dispatchers,
            validateDelivery = { check(current) { "SDK channel changed" } }
        )

        try {
            sut.enqueue(callback)
            testScheduler.advanceUntilIdle()
            assertThat(executed).isTrue()
            verify(exactly = 1) {
                callback.onResult(match {
                    !it.isSuccess() && it.getError() is IllegalStateException
                })
            }
        } finally {
            scope.cancel()
        }
    }
}
