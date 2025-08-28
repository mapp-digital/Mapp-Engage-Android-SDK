package com.appoxee

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Looper
import com.appoxee.internal.AppoxeeImpl
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inbox.InboxMessage
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse
import com.appoxee.internal.model.response.inbox.MessageStatus
import com.appoxee.internal.network.Call
import com.appoxee.internal.util.DispatchersProvider
import com.appoxee.internal.util.DispatchersProviderImpl
import com.appoxee.internal.util.Logger
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.GeoStatus
import com.appoxee.shared.LocalPushBroadcast
import com.google.firebase.messaging.RemoteMessage

/**
 * Engage SDK public API for usage in client's application
 * It defines actions supported by the SDK.
 */
interface Appoxee {
    companion object {
        private val TAG = Appoxee::class.java.name
        private lateinit var mInstance: Appoxee
        private val dispatchersProvider: DispatchersProvider = DispatchersProviderImpl()

        /**
         * Initialization method for the SDK.
         * This method must be called from a main thread.
         * Throws [IllegalAccessException] when called from a background thread
         */
        @JvmStatic
        fun engage(
            context: Context,
            options: AppoxeeOptions? = null,
        ) {
            if (Thread.currentThread() != Looper.getMainLooper().thread) {
                throw IllegalAccessException("Must be called from a main thread!")
            }
            mInstance = AppoxeeImpl(
                application =  context.applicationContext as Application,
                options =  options,
                dispatcherProvider = dispatchersProvider
            )
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
     * @param [resendCustomAttributes] if set to true and alias value was changed compared to the last saved one,
     * all cached custom attributes will be synced again to a backend
     */
    fun setAlias(alias: String, resendCustomAttributes: Boolean = false): Call<String?>

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
     * @return [Boolean] true if push enabled; false if push disabled.
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
     * Get list of inapp messages from server and show them as proper dialog or fullscreen page
     */
    fun triggerInApp(context: Activity, eventName: String): Call<Boolean>

    /**
     * Start Geofence tracking
     * @param enterDelaySeconds number of seconds for enter event to trigger.
     * If set to 0, Enter is triggered, otherwise DWELL is triggered.
     */
    fun <T : GeoStatus> startGeofencing(enterDelaySeconds: Int = 0): Call<T>

    /**
     * Stop Geofence tracking
     */
    fun <T : GeoStatus> stopGeofencing(): Call<T>

    fun isGeofencingActive(): Call<Boolean>

    /**
     * Log out a user. Alias will reset.
     * @param pushEnabled - Enable or disable push messages.
     */
    fun logout(pushEnabled: Boolean): Call<Boolean>

    /**
     * Add list of tags to a device
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

    /**
     * Register Broadcast receiver class from client application.
     * SDK will delegate events related to push messages to this class.
     * Client app can use those events to execute some custom actions.
     */
    fun <T : LocalPushBroadcast> setPushBroadcast(clazz: Class<T>)

    /**
     * Update firebase token for a registered device
     *
     * OptIn/OptOut state won't be changed.
     */
    fun updateFirebaseToken(token: String): Call<Boolean>
}