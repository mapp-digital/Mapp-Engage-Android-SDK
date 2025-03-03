package com.appoxee.internal.container

import com.appoxee.internal.stats.StatsClient
import com.appoxee.internal.ui.inapp.InAppManager
import com.appoxee.internal.ui.inapp.InAppManagerImpl
import com.appoxee.internal.ui.inapp.nativ.NativeFactory
import com.appoxee.internal.ui.inapp.web.WebFactory
import com.appoxee.internal.util.DispatchersProvider
import com.appoxee.internal.util.DispatchersProviderImpl
import kotlinx.coroutines.CoroutineScope

internal class InAppContainer(
    private val scope: CoroutineScope,
    private val statsClient: StatsClient,
    private val actionContainer: ActionContainer,
) {
    internal val dispatchersProvider: DispatchersProvider by lazy { DispatchersProviderImpl() }

    private val nativeFactory: NativeFactory by lazy {
        NativeFactory(scope, dispatchersProvider, actionContainer)
    }

    private val webFactory: WebFactory by lazy {
        WebFactory(scope, dispatchersProvider, actionContainer)
    }

    internal val inappManager: InAppManager by lazy {
        InAppManagerImpl(nativeFactory, webFactory, statsClient, scope, dispatchersProvider)
    }
}