package com.appoxee.internal.ui.push.base

import android.app.Notification

interface Notify {
    fun showNotification(notification: Notification, notificationId: Int)
    fun closeNotification(notificationId: Int)
    fun createChannel(channelId: String, channelName: String, importance:Int)
}