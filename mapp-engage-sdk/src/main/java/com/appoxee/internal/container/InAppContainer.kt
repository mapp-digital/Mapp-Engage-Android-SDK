package com.appoxee.internal.container

import com.appoxee.internal.ui.inapp.InAppManager
import com.appoxee.internal.ui.inapp.InAppManagerImpl
import com.appoxee.internal.ui.inapp.nativ.NativeFactory
import com.appoxee.internal.ui.inapp.web.WebFactory
import com.appoxee.internal.util.Dispatchers
import com.appoxee.internal.util.DispatchersImpl
import kotlinx.coroutines.CoroutineScope

internal class InAppContainer(private val scope: CoroutineScope) {
    internal val dispatchers: Dispatchers by lazy { DispatchersImpl() }

    internal val nativeFactory: NativeFactory by lazy {
        NativeFactory(scope, dispatchers)
    }

    internal val webFactory: WebFactory by lazy {
        WebFactory(scope,dispatchers)
    }

    internal val inappManager: InAppManager by lazy {
        InAppManagerImpl(nativeFactory, webFactory)
    }
}