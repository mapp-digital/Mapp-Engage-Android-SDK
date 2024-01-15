package com.appoxee.internal.container

import android.app.Application
import com.appoxee.internal.storage.PrefsStorageImpl
import com.appoxee.internal.storage.Storage
import java.util.concurrent.TimeUnit

internal class StorageContainer(
    application: Application,
    cacheValidityMs: Long = TimeUnit.DAYS.toMillis(1)
) {
    private val _storage: Storage by lazy { PrefsStorageImpl(application, cacheValidityMs) }
    internal val storage: Storage
        get() = _storage
}
