package com.appoxee.internal.container

import android.content.Context
import com.appoxee.internal.storage.PrefsStorageImpl
import com.appoxee.internal.storage.Storage
import com.appoxee.internal.util.Dispatchers
import com.appoxee.internal.util.DispatchersImpl
import java.util.concurrent.TimeUnit

internal class StorageContainer private constructor(
    context: Context,
    cacheValidityMs: Long,
    dispatchers: Dispatchers
) {
    companion object {
        private lateinit var instance: StorageContainer

        internal fun getInstance(
            context: Context,
            cacheValidityMs: Long = TimeUnit.DAYS.toMillis(1),
            dispatchers: Dispatchers = DispatchersImpl()
        ): StorageContainer {
            if (!::instance.isInitialized) {
                instance =
                    StorageContainer(context, cacheValidityMs, dispatchers)
            }
            return instance
        }
    }

    private val _storage: Storage by lazy {
        PrefsStorageImpl(
            context,
            cacheValidityMs,
            dispatchers
        )
    }
    internal val storage: Storage
        get() = _storage
}
