@file:Suppress("PrivatePropertyName")

package com.appoxee.internal

import android.app.Activity
import android.app.Application
import androidx.annotation.VisibleForTesting
import com.appoxee.Appoxee
import com.appoxee.internal.container.ActionContainer
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.container.InAppContainer
import com.appoxee.internal.container.PushContainer
import com.appoxee.internal.migration.MigrationHelper
import com.appoxee.internal.migration.data.OldRegistration
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inbox.InboxMessage
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse
import com.appoxee.internal.model.response.inbox.MessageStatus
import com.appoxee.internal.network.Call
import com.appoxee.internal.network.HttpCall
import com.appoxee.internal.provider.DeviceProvider
import com.appoxee.internal.storage.Storage
import com.appoxee.internal.ui.custom.MappWebView
import com.appoxee.internal.util.Logger
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.GeoStatus
import com.appoxee.shared.GeofenceException
import com.appoxee.shared.LocalPushBroadcast
import com.appoxee.shared.MappResult
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("UNCHECKED_CAST")
internal class AppoxeeImpl(
    private val application: Application,
    private val options: AppoxeeOptions? = null,
    private val dispatchers: com.appoxee.internal.util.Dispatchers,
    private val internalScope: CoroutineScope
) : Appoxee, AppoxeeObservable {

    private val TAG = AppoxeeImpl::class.java.name

    private val mutex = Mutex()

    private val observers: MutableSet<AppoxeeObserver> by lazy { mutableSetOf() }

    private val mIsReady by lazy { AtomicBoolean(false) }

    private val pushQueue = mutableSetOf<RemoteMessage>()

    private val actionContainer: ActionContainer
        get() = ActionContainer(application)

    private val inappContainer: InAppContainer
        get() = InAppContainer(internalScope, appoxeeContainer.statsClient, actionContainer)


    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val appoxeeContainer = AppoxeeContainer.getInstance(
        context = application, dispatchers = dispatchers
    )

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val appoxeeAdapter: AppoxeeAdapter
        get() = appoxeeContainer.appoxeeAdapter

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val storage: Storage
        get() = appoxeeContainer.storage

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val pushContainer: PushContainer
        get() = PushContainer(application, appoxeeContainer)

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val deviceProvider: DeviceProvider
        get() = appoxeeContainer.deviceProvider

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val migrationHelper: MigrationHelper
        get() = appoxeeContainer.migrationHelper


    init {
        internalScope.launch(dispatchers.ioDispatcher) {
            initializeSdk()
        }
    }

    private suspend fun initializeSdk() {
        Logger.init(application.applicationContext as Application)

        (application.applicationContext as Application).registerActivityLifecycleCallbacks(
            appoxeeContainer.activityLifecycleHandler
        )

        println("OPTIONS: $options")
        // save config to local storage if not null
        options?.let {
            if (it != storage.getInitOptions()) {
                storage.clearRegistration()
                storage.saveInitOptions(it)
            }
        }

        // check device registration
        // update if exist or register new device
        validateRegistration()?.let {
            withContext(dispatchers.mainDispatcher) {
                updateReadyStatus(true, MappResult.Success(it))
            }
        }

        // fetch InApp Configuration parameters
        getAppConfig()

        //init webview
        withContext(dispatchers.mainDispatcher) {
            MappWebView.getInstance(application.applicationContext)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal suspend fun validateRegistration(): DevicePayload? {
        // get saved device from local storage
        var devicePayload: DevicePayload? = storage.getDevicePayload()

        // get registration data used to register device on server
        val savedRegisterPayload = storage.getRegistrationDevice()

        // calculate current registration data of device
        val newRegisterPayload = deviceProvider.generateRegistrationDevice()

        // if local device payload exist and data are not expired
        if (devicePayload?.udidHashed != null /* && not expired */) {
            // check if saved registration data and currently calculated registration data differs
            if (savedRegisterPayload?.equals(newRegisterPayload) == false) {
                // when registration data differs, register data again to update values on server
                appoxeeAdapter.register(newRegisterPayload)

                // update optIn or optOut status with firebase token
                // this fulfills requirement to preserve OptIn/OptOut state when channel changed
                updateOptStatus(devicePayload, null)

                // get device payload from server after new registration
                Logger.d(TAG, "validateRegistration - savedRegistration != newRegistration")
                devicePayload = appoxeeAdapter.getDevice()
            } else {
                // device already registered and channel is unchanged
                // update only FB token if needed
                updateOptStatus(devicePayload, null)
            }
        } else {
            // check if device registered with older version (v6) and needs to be migrated
            val oldOptions = migrationHelper.getRegistrationOptions()

            // get old registration options
            val oldRegistration = migrationHelper.fetchRegistrationData()

            // if channel is unchanged, reuse device registration and do not register device again.
            // migrate registration to SDK v7 structure and delete SDK v6 registration data.
            if (options?.equals(oldOptions) == true) {
                devicePayload = appoxeeAdapter.getDevice()

                // update only optOut state to refresh firebase token if it is changed
                updateOptStatus(devicePayload, oldRegistration)

                // delete old registration data
                migrationHelper.deleteOldRegistration()
            }

            // Device payload is null if device was not previously registered or if channel data were changed.
            // In this case, register new device and update optIn/optOut state
            if (devicePayload?.udidHashed == null) {
                // if device payload doesn't exist after all checkins, register device
                appoxeeAdapter.register(newRegisterPayload)

                // update optIn or optOut status with firebase token
                updateOptStatus(devicePayload, oldRegistration)

                // delete old registration data if still exists
                migrationHelper.deleteOldRegistration()

                // get device payload from server after new registration
                Logger.d(TAG, "validateRegistration - new device registered; udidHashed != null")
                devicePayload = appoxeeAdapter.getDevice()
            }
        }

        // save device registration data (device fingerprint) to a local storage for later access
        storage.saveRegistrationDevice(newRegisterPayload)

        // save device payload from server for a registered device
        storage.saveDevicePayload(devicePayload)

        // returns device payload
        return devicePayload
    }


    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal suspend fun updateOptStatus(
        devicePayload: DevicePayload?,
        oldRegistration: OldRegistration?
    ) {
        Logger.d(TAG, "updateOptStatus()")
        val pushToken = FirebaseMessaging.getInstance().token.await()
        Logger.d(TAG, "PUSH TOKEN: $pushToken")

        // if device opted In and optIn token is expired, update optIn token
        if (devicePayload?.pushToken?.isNotEmpty() == true || oldRegistration?.pushEnabled == true) {
            if (pushToken != devicePayload?.pushToken) {
                appoxeeAdapter.optIn(pushToken)
            }
        } else {
            // if device opted Out and optOut token is expired, update optOut token
            if (pushToken != devicePayload?.pushTokenBk) {
                appoxeeAdapter.optOut(pushToken)
            }
        }

        Logger.d(TAG, "updateOptStatus() - Finished")
    }

    override fun isReady(): Boolean {
        return mIsReady.get()
    }

    override fun setAlias(alias: String): Call<String?> = buildHttpCall {
        appoxeeAdapter.setAlias(alias)
    }

    override fun getAlias(): Call<String?> = buildHttpCall {
        appoxeeAdapter.getAlias()
    }

    override fun fetchInboxMessages(): Call<InboxMessagesResponse?> =
        buildHttpCall {
            appoxeeAdapter.fetchInboxMessages("app_inbox")
        }

    override fun fetchInboxMessage(templateId: Long): Call<InboxMessage?> =
        buildHttpCall {
            val response = appoxeeAdapter.fetchInboxMessages("app_inbox")
            response?.messages?.firstOrNull { it.templateId == templateId }
        }


    override fun fetchLatestInboxMessage(): Call<InboxMessage?> =
        buildHttpCall {
            val response = appoxeeAdapter.fetchInboxMessages("app_inbox")
            response?.messages?.maxByOrNull { it.templateId }
        }

    override fun updateInboxMessageStatus(
        message: InboxMessage,
        status: MessageStatus
    ): Call<Boolean> = buildHttpCall {
        inappContainer.inappManager.markInboxMessageStatus(message, status)
    }

    override fun showInboxMessage(context: Activity, message: InboxMessage) {
        inappContainer.inappManager.showMessage(
            activity = context,
            message = message.getInappMessage()
        )
    }

    override fun fetchInappMessages(eventName: String): Call<InappResponse?> = buildHttpCall {
        appoxeeAdapter.fetchInappMessages(eventName)
    }

    override fun triggerInApp(context: Activity, eventName: String): Call<Boolean> = buildHttpCall {
        val inappResponse = appoxeeAdapter.fetchInappMessages(eventName)
        inappContainer.inappManager.let { inappManager ->
            val sortedMessages = inappManager.parseResponse(inappResponse)
            withContext(dispatchers.mainDispatcher) {
                inappManager.handleMessages(context, sortedMessages)
            }
        }
        true
    }

    override fun <T : GeoStatus> startGeofencing(enterDelaySeconds: Int): Call<T> = buildHttpCall {
        try {
            appoxeeContainer.geoContainer.geofenceRegistry.startGeofencing(enterDelaySeconds)
        } catch (e: GeofenceException) {
            e.geoStatus
        } catch (e: Exception) {
            Logger.d(TAG, "Exception : $e")
            GeoStatus.GeoGeneralError()
        } as T
    }

    override fun <T : GeoStatus> stopGeofencing(): Call<T> = buildHttpCall {
        return@buildHttpCall try {
            appoxeeContainer.geoContainer.geofenceRegistry.stopGeofencing()
        } catch (e: GeofenceException) {
            e.geoStatus
        } catch (e: Exception) {
            Logger.d(TAG, "Exception : $e")
            GeoStatus.GeoErrorStopping()
        } as T
    }

    override fun logout(pushEnabled: Boolean): Call<Boolean> = buildHttpCall {
        val device = appoxeeContainer.deviceProvider.generateRegistrationDevice()
        val response = appoxeeAdapter.logout(device)
        enablePush(pushEnabled)
        response.isSuccess()
    }


    override fun enablePush(enabled: Boolean, token: String?): Call<Boolean> = buildHttpCall {
        val fbToken = token ?: FirebaseMessaging.getInstance().token.await()
        if (enabled) {
            appoxeeAdapter.optIn(fbToken)
        } else {
            appoxeeAdapter.optOut(fbToken)
        }
    }

    override fun isPushEnabled(): Call<Boolean> = buildHttpCall {
        val devicePayload = storage.getDevicePayload()
        !devicePayload?.pushToken.isNullOrEmpty()
    }

    override fun getFirebaseToken(): Call<String?> = buildHttpCall {
        storage.getDevicePayload()?.pushToken
    }

    override fun addTags(tags: List<String>): Call<Boolean> = buildHttpCall {
        appoxeeAdapter.addTags(tags).isSuccess()
    }

    override fun removeTags(tags: List<String>): Call<Boolean> = buildHttpCall {
        appoxeeAdapter.removeTags(tags).isSuccess()
    }

    override fun addCustomAttributes(attributes: Map<String, Any?>): Call<Boolean> = buildHttpCall {
        appoxeeAdapter.addCustomAttributes(attributes).isSuccess()
    }

    override fun getCustomAttributes(attributes: List<String>): Call<Map<String, Any?>> =
        buildHttpCall {
            val response = appoxeeAdapter.getCustomAttributes(attributes)
            return@buildHttpCall response.data?.payload ?: emptyMap()
        }

    override fun getDevice(): Call<DevicePayload?> = buildHttpCall {
        appoxeeAdapter.getDevice()
    }


    override fun updateReadyStatus(status: Boolean, mappResult: MappResult<DevicePayload>) {
        mIsReady.set(status)
        observers.forEach {
            it.onReadyStatusChanged(status, mappResult)
        }
        pushQueue.forEach {
            internalScope.launch {
                pushContainer.pushManager.handlePushMessage(application.applicationContext, it)
            }
        }
    }

    override fun subscribe(observer: AppoxeeObserver) {
        internalScope.launch {
            mutex.withLock {
                val payload = storage.getDevicePayload()
                withContext(dispatchers.mainDispatcher) {
                    observers.add(observer)
                    val device = payload ?: return@withContext
                    observer.onReadyStatusChanged(
                        isReady(), MappResult.Success(data = device)
                    )
                }
            }
        }
    }

    override fun unsubscribe(observer: AppoxeeObserver) {
        observers.remove(observer)
    }

    override fun handlePushMessage(remoteMessage: RemoteMessage) {
        internalScope.launch {
            mutex.withLock {
                if (mIsReady.get()) {
                    pushContainer.pushManager.handlePushMessage(
                        application.applicationContext,
                        remoteMessage
                    )
                } else {
                    pushQueue.add(remoteMessage)
                }
            }
        }
    }

    override fun isPushMessageFromMapp(remoteMessage: RemoteMessage): Boolean {
        return pushContainer.pushManager.isPushMessageFromMapp(remoteMessage)
    }

    private suspend fun getAppConfig() {
        withContext(dispatchers.ioDispatcher) {
            val result = appoxeeAdapter.getAppConfig()
            if (result.isSuccess()) {
                storage.saveAppConfig(result.data?.payload)
                Logger.d(TAG, "APP CONFIG: ${result.data?.toString()}")
            } else {
                Logger.e(TAG, result.error?.toString() ?: "Error")
            }
        }
    }

    override fun closeNotification(notificationId: Int) {
        pushContainer.pushManager.dismissNotification(notificationId = notificationId)
    }

    override fun <T : LocalPushBroadcast> setPushBroadcast(clazz: Class<T>) {
        internalScope.launch {
            val requiredClass = LocalPushBroadcast::class.java
            if (clazz.superclass == requiredClass) {
                appoxeeContainer.localPushBroadcast = clazz
                withContext(dispatchers.ioDispatcher) {
                    storage.setBroadcastClass(clazz)
                }

            } else {
                throw IllegalArgumentException("PushBroadcast must be subtype of LocalPushBroadcast")
            }
        }
    }

    private fun <T> buildHttpCall(
        call: suspend () -> T
    ): Call<T> {
        return HttpCall(coroutineScope = appoxeeContainer.baseScope, call)
    }
}