package com.appoxee.internal.container

import com.appoxee.internal.ui.inapp.InAppManager
import com.appoxee.internal.ui.inapp.InAppManagerImpl
import com.appoxee.internal.ui.inapp.nativ.NativeTemplateFactory
import com.appoxee.internal.ui.inapp.web.WebTemplateFactory
import kotlinx.coroutines.CoroutineScope

internal class InAppContainer(private val scope: CoroutineScope) {
    internal val nativeTemplateFactory: NativeTemplateFactory by lazy {
        NativeTemplateFactory(scope)
    }

    internal val webTemplateFactory: WebTemplateFactory by lazy {
        WebTemplateFactory(scope)
    }

    internal val inappManager: InAppManager by lazy {
        InAppManagerImpl(nativeTemplateFactory, webTemplateFactory)
    }
}