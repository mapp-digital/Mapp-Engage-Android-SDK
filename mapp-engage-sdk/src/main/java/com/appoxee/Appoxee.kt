package com.appoxee

import android.content.Context
import com.appoxee.internal.AppoxeeImpl
import com.appoxee.internal.model.request.geo.GeoEvent
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.ResponseData
import com.appoxee.internal.model.response.geo.RegionsResponse
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse
import com.appoxee.internal.network.Call
import com.appoxee.internal.network.response.Response
import com.appoxee.internal.util.Logger
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.AppoxeeOptions
import com.google.firebase.messaging.RemoteMessage
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.annotations.TestOnly

interface Appoxee {
    companion object {
        private val TAG = Appoxee::class.java.name
        private lateinit var mInstance: Appoxee

        @JvmStatic
        fun engage(
            context: Context,
            options: AppoxeeOptions,
        ) {
            mInstance = AppoxeeImpl(context.applicationContext, options)
            Logger.d(TAG, "engage($context, $options)")
        }

//        internal fun engage(context: Context) {
//            if (!::mInstance.isInitialized) {
//                mInstance = AppoxeeImpl(context.applicationContext)
//            }
//            Logger.d(TAG, "engage($context)")
//        }

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

    fun enablePush(enabled: Boolean): Call<Boolean>

    fun fetchInboxMessages(
        eventName: String,
    ): Call<InboxMessagesResponse?>

    fun fetchInappMessages(
        eventName: String,
    ): Call<InappResponse?>

    fun addTags(tags: List<String>): Call<Boolean>

    fun removeTags(tags: List<String>): Call<Boolean>

    fun addCustomAttributes(attributes: Map<String, Any?>): Call<Boolean>

    fun getCustomAttributes(attributes: List<String>): Call<Map<String, Any?>>

    fun subscribe(observer: AppoxeeObserver)

    fun unsubscribe(observer: AppoxeeObserver)

    fun handlePushMessage(remoteMessage: RemoteMessage)

    fun isPushMessageFromMapp(remoteMessage: RemoteMessage): Boolean

    fun closeNotification(notificationId:Int)

    @TestOnly
    fun testCall(): Call<String>

    @TestOnly
    fun testActivate(): Call<Boolean>

    @TestOnly
    fun testInappEvent(): Call<Boolean>

    @TestOnly
    fun testPushEvent(): Call<Boolean>

    @TestOnly
    fun testGetRegions(
        lat: Double,
        lng: Double,
        version: Int,
        pageSize: Int
    ): Call<RegionsResponse>

    @TestOnly
    fun testRegionEvent(
        geoEvent: GeoEvent,
        latitude: Double,
        longitude: Double,
        regionId: Long,
        version: Int
    ): Call<Boolean>
}