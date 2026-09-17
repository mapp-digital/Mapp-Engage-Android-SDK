package com.appoxee.internal.storage

import com.appoxee.internal.model.response.AppConfigPayload
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.network.EngageApi
import com.appoxee.internal.util.DispatchersProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Refresh expired data on access, retaining the last known values on failure. */
internal class RefreshingStorage(
    private val delegate: Storage,
    private val api: () -> EngageApi,
    private val dispatchers: DispatchersProvider,
    private val now: () -> Long = System::currentTimeMillis,
    private val ttlMs: Long = TimeUnit.DAYS.toMillis(1)
) : Storage by delegate {
    private val deviceMutex = Mutex()
    private val configMutex = Mutex()
    private val deviceAttempts = AtomicLong()
    private val configAttempts = AtomicLong()

    private fun isFresh(timestamp: Long): Boolean =
        timestamp > 0 && now() - timestamp in 0 until ttlMs

    override suspend fun peekDevicePayload(): DevicePayload? = delegate.getDevicePayload()

    override suspend fun getDevicePayload(): DevicePayload? {
        val attempt = deviceAttempts.get()
        return deviceMutex.withLock {
            val cached = delegate.getDevicePayload()
            // No cached identity means registration has not completed yet.
            if (cached == null || isFresh(delegate.getDeviceTimestamp()) ||
                deviceAttempts.get() != attempt) return@withLock cached
            try {
                val response = withContext(dispatchers.ioDispatcher) { api().getDevice() }
                val payload = response.data?.payload
                if (response.isSuccess() && response.data?.metadata?.error != true &&
                    payload?.udidHashed != null) {
                    delegate.saveRefreshedDevicePayload(payload)
                    return@withLock payload
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep the expired timestamp so a later access can retry.
            } finally {
                deviceAttempts.incrementAndGet()
            }
            cached
        }
    }

    override suspend fun getAppConfig(): AppConfigPayload? {
        val attempt = configAttempts.get()
        return configMutex.withLock {
            val cached = delegate.getAppConfig()
            if ((cached != null && isFresh(delegate.getTimestamp())) ||
                configAttempts.get() != attempt) return@withLock cached
            try {
                val response = withContext(dispatchers.ioDispatcher) { api().getAppConfig() }
                val payload = response.data?.payload
                if (response.isSuccess() && response.data?.metadata?.error != true && payload != null) {
                    delegate.saveAppConfig(payload)
                    delegate.updateCacheTimestamp()
                    return@withLock payload
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep cached configuration when offline or when the server rejects the request.
            } finally {
                configAttempts.incrementAndGet()
            }
            cached
        }
    }

    override suspend fun saveDevicePayload(devicePayload: DevicePayload?) {
        deviceMutex.withLock { delegate.saveDevicePayload(devicePayload) }
    }

    override suspend fun saveRefreshedDevicePayload(devicePayload: DevicePayload) {
        deviceMutex.withLock { delegate.saveRefreshedDevicePayload(devicePayload) }
    }

    override suspend fun saveAppConfig(appConfigPayload: AppConfigPayload?) {
        configMutex.withLock {
            delegate.saveAppConfig(appConfigPayload)
            if (appConfigPayload != null) delegate.updateCacheTimestamp()
        }
    }

    override suspend fun clearRegistration() {
        deviceMutex.withLock {
            configMutex.withLock { delegate.clearRegistration() }
        }
    }
}
