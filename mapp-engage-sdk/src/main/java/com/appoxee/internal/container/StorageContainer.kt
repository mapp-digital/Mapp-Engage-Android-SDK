package com.appoxee.internal.container

import android.app.Application
import com.appoxee.internal.storage.PrefsStorageImpl
import com.appoxee.internal.storage.Storage

internal class StorageContainer(application: Application, cacheValidityMs: Long) {
    internal val storage: Storage by lazy { PrefsStorageImpl(application, cacheValidityMs) }
}
