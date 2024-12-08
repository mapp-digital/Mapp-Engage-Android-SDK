package com.appoxee

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Looper
import com.appoxee.internal.AppoxeeImpl
import com.appoxee.internal.model.request.geo.GeoEvent
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.geo.RegionsResponse
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inbox.InboxMessage
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse
import com.appoxee.internal.model.response.inbox.MessageStatus
import com.appoxee.internal.network.Call
import com.appoxee.internal.util.DispatchersImpl
import com.appoxee.internal.util.Logger
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.GeoStatus
import com.appoxee.shared.LocalPushBroadcast
import com.google.firebase.messaging.RemoteMessage
import org.jetbrains.annotations.TestOnly

/**
 * Engage SDK public API for usage in client's application
 * It defines actions supported by the SDK.
 */
interface Appoxee {
    companion object {
        private val TAG = Appoxee::class.java.name
        private lateinit var mInstance: Appoxee
        private val dispatchers: com.appoxee.internal.util.Dispatchers = DispatchersImpl()

        /**
         * Initialization method for the SDK.
         * This method must be called from a main thread.
         * Throws [IllegalAccessException] when called from a background thread
         */
        @JvmStatic
        fun engage(
            context: Context,
            options: AppoxeeOptions,
        ) {
            if (Thread.currentThread() != Looper.getMainLooper().thread) {
                throw IllegalAccessException("Must be called from a main thread!")
            }
            mInstance = AppoxeeImpl(context.applicationContext as Application, options, dispatchers)
            Logger.d(TAG, "engage($context, $options)")
        }

        /**
         * @returns singleton instance of Engage SDK.
         * Throws [NullPointerException] if SDK is not initialized (if [engage] was not called first).
         */
        @Throws(NullPointerException::class)
        @JvmStatic
        fun instance(): Appoxee {
            if (!::mInstance.isInitialized) {
                throw NullPointerException("Must call engage() first!!!")
            }
            return mInstance
        }
    }

    /**
     * Checks if initialization is completed and SDK is ready to use.
     * @return [Boolean] ready state as true if SDK is ready to use; otherwise returns false.
     */
    fun isReady(): Boolean

    /**
     * Get data for the registered device.
     * @return [DevicePayload] instance if device is successfully registered.
     */
    fun getDevice(): Call<DevicePayload?>

    /**
     * Set custom alias for a registered device/user
     * @param alias custom alias to set
     * @return [String] dmcDeviceId string identification for a registered device if setAlias was successful
     */
    fun setAlias(alias: String): Call<String?>

    /**
     * Get alias for a registered device
     * @return [String] alias for a registered device
     */
    fun getAlias(): Call<String?>

    /**
     * Opt In or Opt Out device to receive push messages or not.
     * @param enabled true to OptIn device. false to OptOut device.
     * @param token firebase client token to subscribe device on Firebase service.
     * <b>This token shouldn't be sent for a regular usage.
     * It's primary purpose is for use case where [MappMessagingService] is disabled, and client has it's own service
     * for handling firebase push messaging. </b>
     * @return [Boolean] true if push enabled; false if not.
     */
    fun enablePush(enabled: Boolean, token: String? = null): Call<Boolean>

    /**
     * Checks if device is opted in or not
     * @return true if device is opted in, or false if device is opted out.
     */
    fun isPushEnabled(): Call<Boolean>

    /**
     * Gets current firebase client token used for device subscribe on a Firebase
     * @return [String] firebase client token of a device
     */
    fun getFirebaseToken(): Call<String?>

    /**
     * Get list of active inbox messages
     * @return [InboxMessagesResponse] that holds list of inbox messages
     */
    fun fetchInboxMessages(
    ): Call<InboxMessagesResponse?>

    /**
     * Get inbox messages of requested templateId
     * @param templateId to filter inbox messages
     * @return [InboxMessage] the inbox message with a requested templateId
     */
    fun fetchInboxMessage(templateId: Long): Call<InboxMessage?>

    /**
     * Get latest inbox message
     * @return [InboxMessage] the latest inbox message
     */
    fun fetchLatestInboxMessage(): Call<InboxMessage?>

    /**
     * Update status of an inbox message
     * @param message [InboxMessage] instance of Inbox Message to be updated
     * @status [MessageStatus] status to update to a message
     * @return if update was successful
     */
    fun updateInboxMessageStatus(message: InboxMessage, status: MessageStatus): Call<Boolean>

    /**
     * Show inbox message as banner, dialog or fullscreen page
     * For native templates it will show type set in the Inapp message.
     * For web templates it will show Inapp message as dialog
     */
    fun showInboxMessage(context: Activity, message: InboxMessage)

    /**
     * Get list of active inapp messages
     * @param eventName to filter inapp messages
     * @return [InappResponse] that holds list of inapp messages
     */
    fun fetchInappMessages(
        eventName: String,
    ): Call<InappResponse?>

    /**
     * Get list of inapp messages from server and show them as proper dialog or fullscreen page
     */
    fun triggerInApp(context: Activity, eventName: String)

    fun <T : GeoStatus> startGeofencing(): T

    fun <T : GeoStatus> stopGeofencing(): T

    /**
     * Add list of tags on a device
     * @param tags list of tags to add
     * @return [Boolean] status of method execution. True if successful, otherwise false.
     */
    fun addTags(tags: List<String>): Call<Boolean>

    /**
     * Remove list of tags from a device
     * @param tags list of tags to remove
     * @return [Boolean] status of method execution. True if successful, otherwise false.
     */
    fun removeTags(tags: List<String>): Call<Boolean>

    /**
     * Add custom attributes to a device
     * @param attributes map of attributes to add
     * @return [Boolean] status of method execution. True if successful, otherwise false.
     */
    fun addCustomAttributes(attributes: Map<String, Any?>): Call<Boolean>

    /**
     * Checks if custom attributes are added and exists on a device
     * @param attributes map of attributes to search for
     * @return [Map] of existing attributes that are requested
     */
    fun getCustomAttributes(attributes: List<String>): Call<Map<String, Any?>>

    /**
     * Subscribe an observer to a SDK status updates.
     * Subscriber will get immediate update with a current status upon subscription.
     * @param instance that implements [AppoxeeObserver] interface
     */
    fun subscribe(observer: AppoxeeObserver)

    /**
     * Unsubscribe an observer from SDK status updates
     * @param instance that implements [AppoxeeObserver] interface
     */
    fun unsubscribe(observer: AppoxeeObserver)

    /**
     * Handle Mapp's push messages, received by other [FirebaseMessagingService].
     * Intended for use case when [MappMessagingService] is disabled
     * and some other service is active to receive Firebase Push Messages.
     */
    fun handlePushMessage(remoteMessage: RemoteMessage)

    /**
     * Checks if received push message is sent from Mapp service
     * @param remoteMessage Firebase Push Message
     * @return [Boolean] true if message is from Mapp service; otherwise false.
     */
    fun isPushMessageFromMapp(remoteMessage: RemoteMessage): Boolean

    /**
     * Close notification with a provided notificationId
     * @param notificationId for a notification to close
     */
    fun closeNotification(notificationId: Int)

    fun <T : LocalPushBroadcast> setPushBroadcast(clazz: Class<T>)

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