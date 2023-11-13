package com.appoxee

import android.content.Context
import com.appoxee.internal.AppoxeeImpl
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse
import com.appoxee.internal.network.Call
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.AppoxeeOptions

interface Appoxee {
    companion object {
        private lateinit var mInstance: Appoxee

        @JvmStatic
        fun engage(
            context: Context,
            options: AppoxeeOptions,
        ) {
            mInstance = AppoxeeImpl(context.applicationContext, options)
        }

        @JvmStatic
        fun instance(): Appoxee {
            if (!::mInstance.isInitialized) {
                throw NullPointerException("Must call engage() first!!!")
            }
            return mInstance
        }
    }

    fun isReady(): Boolean

    fun getDevice(): Call<DevicePayload?>

    fun setAlias(alias: String): Call<String?>

    fun getAlias(): Call<String?>

    fun fetchInboxMessages(
        eventName: String,
    ): Call<InboxMessagesResponse?>

    fun fetchInappMessages(
        eventName: String,
    ): Call<InappResponse?>

    fun optIn(token: String): Call<Boolean>

    fun optOut(token: String): Call<Boolean>

    fun addTags(tags: List<String>): Call<Boolean>

    fun removeTags(tags: List<String>): Call<Boolean>

    fun addCustomAttributes(attributes: Map<String, Any?>): Call<Boolean>

    fun getCustomAttributes(attributes: List<String>): Call<Map<String, Any?>>

    fun subscribe(observer: AppoxeeObserver)

    fun unsubscribe(observer: AppoxeeObserver)

    fun testCall(): Call<String>

    fun testInappEvent(): Call<Boolean>

    fun testPushEvent(): Call<Boolean>
}