package com.appoxee.internal

import com.appoxee.shared.AppoxeeOptions

/** Configuration and outcome of one SDK initialization, independent of observer timing. */
internal class SdkInitialization(options: AppoxeeOptions?) {
    enum class Status { INITIALIZING, READY, FAILED }

    @Volatile
    var configuration: AppoxeeOptions? = options?.engagementSnapshot()
        private set

    @Volatile
    var status = Status.INITIALIZING
        private set

    fun canReuse(requested: AppoxeeOptions?): Boolean =
        status != Status.FAILED &&
            (requested == null || configuration?.sameEngagement(requested) == true)

    fun hasSameChannel(other: SdkInitialization): Boolean {
        val first = configuration
        val second = other.configuration
        if (first == null || second == null) return first == null && second == null
        return first.server.value == second.server.value && first.sdkKey == second.sdkKey &&
            first.appId == second.appId && first.tenantId == second.tenantId
    }

    fun resolveSavedConfiguration(saved: AppoxeeOptions?) {
        if (configuration == null) configuration = saved?.engagementSnapshot()
    }

    fun complete(ready: Boolean) {
        status = if (ready) Status.READY else Status.FAILED
    }
}
