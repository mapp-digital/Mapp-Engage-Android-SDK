package com.appoxee.internal.container

import android.app.Application

/** Owned by the process container, independently of replaceable SDK instances. */
internal class LifecycleCallbacksRegistration {
    private var application: Application? = null
    private var callbacks: Application.ActivityLifecycleCallbacks? = null

    @Synchronized
    fun registerOnce(application: Application, callbacks: Application.ActivityLifecycleCallbacks) {
        if (this.application === application && this.callbacks === callbacks) return
        check(this.application == null) { "Lifecycle registration belongs to another application or listener" }
        application.registerActivityLifecycleCallbacks(callbacks)
        // A platform failure must leave the next initialization able to retry.
        this.application = application
        this.callbacks = callbacks
    }
}
