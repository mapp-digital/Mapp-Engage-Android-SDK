package com.appoxee.shared

interface AppoxeeObserver {
    fun onReadyStatusChanged(status: Boolean)
}