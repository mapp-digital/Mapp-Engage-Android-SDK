package com.appoxee.internal.container

import com.appoxee.internal.stats.StatsClient
import com.appoxee.internal.ui.inapp.InAppManager
import com.appoxee.internal.ui.inapp.InAppManagerImpl
import com.appoxee.internal.ui.inapp.nativ.NativeFactory
import com.appoxee.internal.ui.inapp.web.WebFactory
import com.appoxee.internal.util.DispatchersProvider
import com.appoxee.internal.util.DispatchersProviderImpl
import com.appoxee.internal.util.Logger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

internal class InAppContainer(
    private val statsClient: StatsClient,
    private val actionContainer: ActionContainer,
) {
    internal val dispatchersProvider: DispatchersProvider by lazy { DispatchersProviderImpl() }

    private val scope = CoroutineScope(SupervisorJob() + CoroutineExceptionHandler { _, t ->
        Logger.e(this.javaClass.name, t)
    })

    private val nativeFactory: NativeFactory by lazy {
        NativeFactory(scope, dispatchersProvider, actionContainer)
    }

    private val webFactory: WebFactory by lazy {
        WebFactory(scope, dispatchersProvider, actionContainer)
    }

    internal val inappManager: InAppManager by lazy {
        InAppManagerImpl(nativeFactory, webFactory, statsClient, scope)
    }
}