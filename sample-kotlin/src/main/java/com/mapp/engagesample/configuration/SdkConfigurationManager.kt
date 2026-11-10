package com.mapp.engagesample.configuration

import androidx.annotation.MainThread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal enum class ConfigurationStatus { APPLYING, READY, FAILED }
internal enum class ConfigurationSource { BUILD_DEFAULTS, SAVED, UNSAVED }

internal data class ConfigurationState(
    val configuration: SdkConfiguration,
    val status: ConfigurationStatus,
    val source: ConfigurationSource,
    val error: String? = null,
)

/** Application lifetime lets an apply/save finish after rotation or leaving the screen. */
internal class SdkConfigurationManager(
    val defaults: SdkConfiguration,
    private val storage: SdkConfigurationStorage,
    private val client: SdkConfigurationClient,
    private val scope: CoroutineScope,
) {
    private val saved = storage.load()
    private val mutableState = MutableStateFlow(ConfigurationState(
        configuration = saved ?: defaults,
        status = ConfigurationStatus.APPLYING,
        source = if (saved == null) ConfigurationSource.BUILD_DEFAULTS else ConfigurationSource.SAVED,
    ))
    val state: StateFlow<ConfigurationState> = mutableState.asStateFlow()
    private val saveMutex = Mutex()
    private var generation = 0L
    private var started = false
    private var pending: Request? = null

    private data class Request(val id: Long, val configuration: SdkConfiguration, val persist: Boolean)

    @MainThread
    fun start() {
        if (started) return
        started = true
        begin(mutableState.value.configuration, persist = false)
    }

    @MainThread
    fun apply(configuration: SdkConfiguration) {
        require(configuration.hasRequiredValues()) { "SDK key, app ID and tenant ID are required" }
        begin(configuration, persist = true)
    }

    private fun begin(configuration: SdkConfiguration, persist: Boolean) {
        val request = Request(++generation, configuration, persist)
        pending = request
        mutableState.value = ConfigurationState(
            configuration, ConfigurationStatus.APPLYING,
            if (persist) ConfigurationSource.UNSAVED else mutableState.value.source,
        )
        try {
            client.apply(configuration) { ready, error ->
                scope.launch {
                    if (generation != request.id || pending !== request) return@launch
                    if (ready && client.isReady()) complete(request)
                    else if (error != null) fail(request, error)
                }
            }
            // Unchanged, already-ready engagement may not emit another readiness event.
            if (client.isReady()) complete(request)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            fail(request, error)
        }
    }

    private fun complete(request: Request) {
        if (generation != request.id || pending !== request) return
        pending = null
        if (!request.persist) {
            mutableState.value = mutableState.value.copy(status = ConfigurationStatus.READY)
            return
        }
        scope.launch {
            try {
                val savedOverride = request.configuration.takeUnless { it == defaults }
                // A slow save for A must finish before a later successful save for B.
                saveMutex.withLock { storage.save(savedOverride) }
                if (generation != request.id) return@launch
                mutableState.value = ConfigurationState(
                    request.configuration, ConfigurationStatus.READY,
                    if (savedOverride == null) ConfigurationSource.BUILD_DEFAULTS else ConfigurationSource.SAVED,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == request.id) {
                    mutableState.value = mutableState.value.copy(
                        status = ConfigurationStatus.READY,
                        source = ConfigurationSource.UNSAVED,
                        error = error.message ?: "Could not save settings on this device",
                    )
                }
            }
        }
    }

    private fun fail(request: Request, error: Throwable) {
        if (generation != request.id || pending !== request) return
        pending = null
        mutableState.value = mutableState.value.copy(
            status = ConfigurationStatus.FAILED,
            error = error.message ?: "SDK initialization failed",
        )
    }
}
