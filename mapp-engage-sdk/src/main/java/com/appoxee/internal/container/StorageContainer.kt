package com.appoxee.internal.container

import android.content.Context
import com.appoxee.internal.storage.PrefsStorageImpl
import com.appoxee.internal.storage.Storage
import java.util.concurrent.TimeUnit

internal class StorageContainer private constructor(
    context: Context,
    cacheValidityMs: Long
) {
    companion object {
        private lateinit var instance: StorageContainer

        internal fun getInstance(
            context: Context,
            cacheValidityMs: Long = TimeUnit.DAYS.toMillis(1)
        ): StorageContainer {
            if (!::instance.isInitialized) {
                instance =
                    StorageContainer(context, cacheValidityMs)
            }
            return instance
        }
    }

    private val _storage: Storage by lazy { PrefsStorageImpl(context, cacheValidityMs) }
    internal val storage: Storage
        get() = _storage
}
