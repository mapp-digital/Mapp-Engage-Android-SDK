@file:Suppress("PrivatePropertyName")

package com.appoxee.internal

import android.app.Activity
import android.app.Application
import androidx.annotation.Keep
import androidx.annotation.VisibleForTesting
import com.appoxee.Appoxee
import com.appoxee.internal.container.ActionContainer
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.container.InAppContainer
import com.appoxee.internal.container.PushContainer
import com.appoxee.internal.migration.MigrationHelper
import com.appoxee.internal.migration.data.OldRegistration
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.inbox.InboxMessage
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse
import com.appoxee.internal.model.response.inbox.MessageStatus
import com.appoxee.internal.network.Call
import com.appoxee.internal.network.HttpCall
import com.appoxee.internal.provider.DeviceProvider
import com.appoxee.internal.provider.ObserversProvider
import com.appoxee.internal.storage.Storage
import com.appoxee.internal.util.DispatchersProvider
import com.appoxee.internal.util.Logger
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.GeoStatus
import com.appoxee.shared.GeofenceException
import com.appoxee.shared.LocalPushBroadcast
import com.appoxee.shared.MappResult
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("UNCHECKED_CAST")
@Keep
internal open class AppoxeeImpl(
    internal val application: Application,
    private val options: AppoxeeOptions?,
    private val dispatcherProvider: DispatchersProvider,
    val observersProvider: ObserversProvider = ObserversProvider(),
    val appoxeeContainer: AppoxeeContainer = AppoxeeContainer.getInstance(
        application,
        dispatcherProvider
    ),
) : Appoxee, AppoxeeObservable {

    private val TAG = AppoxeeImpl::class.java.name

    private val mutex = Mutex()

    internal val mIsReady by lazy { AtomicBoolean(false) }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val pushQueue by lazy { ConcurrentLinkedQueue<RemoteMessage>() }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val internalScope =
        CoroutineScope(SupervisorJob() + dispatcherProvider.mainDispatcher + CoroutineExceptionHandler { coroutineContext, throwable ->
            Logger.e(this.javaClass.name, "exception in sdk init: $throwable")
            observersProvider.notify(isReady(), MappResult.Error(throwable))
        })

    private val actionContainer: ActionContainer
        get() = ActionContainer(application)

    private val inappContainer: InAppContainer
        get() = InAppContainer(appoxeeContainer.statsClient, actionContainer)

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val pushContainer: PushContainer by lazy {
        PushContainer(
            application,
            appoxeeContainer
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val appoxeeAdapter: AppoxeeAdapter by lazy { appoxeeContainer.appoxeeAdapter }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal open val storage: Storage by lazy { appoxeeContainer.storage }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val deviceProvider: DeviceProvider by lazy { appoxeeContainer.deviceProvider }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val migrationHelper: MigrationHelper by lazy { appoxeeContainer.migrationHelper }


    init {
        // initialize logger
        Logger.init(application)

        // attach activity lifecycle listener
        application.registerActivityLifecycleCallbacks(
            appoxeeContainer.activityLifecycleHandler
        )

        internalScope.launch {
            // initialize sdk
            initializeSdk()
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal suspend fun initializeSdk() = withContext(dispatcherProvider.defaultDispatcher) {
        Logger.d(TAG, "OPTIONS provided: ${options != null}")
        // save config to local storage if not null
        if (options != null) {
            // save only if current options are changed compared to the saved one
            if (!options.areEquals(storage.getInitOptions())) {
                storage.clearRegistration()
                storage.saveInitOptions(options)
            }
        } else {
            if (storage.getInitOptions() == null) {
                throw IllegalStateException("Engage SDK wasn't supplied with initialization parameters!")
            }
        }

        // check device registration
        // update if exist or register new device
        validateRegistration()?.let {
            updateReadyStatus(true, MappResult.Success(it))
        }

        // fetch InApp Configuration parameters
        fetchAppConfig()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal suspend fun validateRegistration(): DevicePayload? {
        // get cached device from local storage
        var devicePayload: DevicePayload? = storage.getDevicePayload()

        // get cached registration data used to register device on server
        val savedRegisterPayload = storage.getRegistrationDevice()

        // calculate current registration data of device
        val newRegisterPayload = deviceProvider.generateRegistrationDevice()

        // if local device payload exist and data are not expired
        if (devicePayload?.udidHashed != null /* && not expired */) {
            Logger.d(TAG, "has device payload")
            // check if saved registration data and currently calculated registration data differs
            if (savedRegisterPayload != null) {
                Logger.d(TAG, "has registration payload")
                val updatedParams = savedRegisterPayload.getChangedParams(newRegisterPayload)
                val alias = devicePayload.alias

                if (!alias.isNullOrEmpty() && updatedParams.isNotEmpty()) {
                    appoxeeAdapter.updateDevice(alias = alias, params = updatedParams)
                }
                // device already registered and channel is unchanged
                updateOptStatus(devicePayload, null)
            } else {
                // when registration data differs, register data again to update values on server
                appoxeeAdapter.register(newRegisterPayload)

                // update optIn or optOut status with firebase token
                // this fulfills requirement to preserve OptIn/OptOut state when channel changed
                updateOptStatus(devicePayload, null)

                // get device payload from server after new registration
                Logger.d(TAG, "validateRegistration - savedRegistration != newRegistration")
                devicePayload = appoxeeAdapter.getDevice()
            }
        } else {
            // check if device registered with older version (v6) and needs to be migrated
            val oldOptions = migrationHelper.getRegistrationOptions()

            // get old registration options
            val oldRegistration = migrationHelper.fetchRegistrationData()

            // if channel is unchanged, reuse device registration and do not register device again.
            // migrate registration to SDK v7 structure and delete SDK v6 registration data.
            if (options?.areEquals(oldOptions) == true) {
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
        val pushToken = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Logger.e(TAG, "updateOptStatus() - failed to fetch push token: $e")
            return
        }
        Logger.d(TAG, "PUSH TOKEN length: ${pushToken.length}")

        try {
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
        } catch (e: Exception) {
            Logger.e(TAG, "updateOptStatus() - failed to update opt status: $e")
        }

        Logger.d(TAG, "updateOptStatus() - Finished")
    }

    override fun isReady(): Boolean {
        return mIsReady.get()
    }

    override fun setAlias(alias: String, resendCustomAttributes: Boolean): Call<String?> =
        buildHttpCall {
            appoxeeAdapter.setAlias(alias, resendCustomAttributes)?.dmcUserId
        }

    override fun getAlias(): Call<String?> = buildHttpCall {
        appoxeeAdapter.getAlias()
    }

    override fun fetchInboxMessages(): Call<InboxMessagesResponse?> = buildHttpCall {
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

    override fun triggerInApp(context: Activity, eventName: String): Call<Boolean> = buildHttpCall {
        val inappResponse = appoxeeAdapter.fetchInappMessages(eventName)
        inappContainer.inappManager.let { inappManager ->
            val sortedMessages = inappManager.parseResponse(inappResponse)
            withContext(dispatcherProvider.mainDispatcher) {
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

    override fun isGeofencingActive(): Call<Boolean> = buildHttpCall {
        val geoContainer = appoxeeContainer.geoContainer
        val isWorkerActive = geoContainer.geofenceScheduler.isGeofencingActive()
        //val isPendingIntentActive = geoContainer.geofenceClient.isGeofencingActive()

        isWorkerActive /*&& isPendingIntentActive*/
    }

    override fun logout(pushEnabled: Boolean): Call<Boolean> = buildHttpCall {
        storage.saveDevicePayload(null)
        storage.saveRegistrationDevice(null)
        val device = appoxeeContainer.deviceProvider.generateRegistrationDevice()
        appoxeeAdapter.logout(device)
        updateOptState(pushEnabled, null)
        true
    }

    override fun enablePush(enabled: Boolean, token: String?): Call<Boolean> = buildHttpCall {
        updateOptState(enabled, token)
    }

    override fun isPushEnabled(): Call<Boolean> = buildHttpCall {
        val devicePayload = storage.getDevicePayload()
        !devicePayload?.pushToken.isNullOrEmpty()
    }

    override fun getFirebaseToken(): Call<String?> = buildHttpCall {
        storage.getDevicePayload()?.pushToken
    }

    override fun addTags(tags: Set<String>): Call<Boolean> = buildHttpCall {
        appoxeeAdapter.addTags(tags).isSuccess()
    }

    override fun removeTags(tags: Set<String>): Call<Boolean> = buildHttpCall {
        appoxeeAdapter.removeTags(tags).isSuccess()
    }

    override fun getTags(): Call<List<String>> = buildHttpCall {
        appoxeeAdapter.getTags()
    }

    override fun addCustomAttributes(attributes: Map<String, Any?>): Call<Boolean> = buildHttpCall {
        val result = appoxeeAdapter.addCustomAttributes(attributes)

        if (result.isSuccess()) {
            true
        } else {
            throw result.error ?: Throwable("Unknown error")
        }
    }


    private suspend fun updateOptState(enabled: Boolean, token: String?): Boolean {
        val fbToken = try {
            token ?: FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Logger.e(TAG, "updateOptState() - failed to fetch push token: $e")
            return false
        }
        return try {
            if (enabled) {
                appoxeeAdapter.optIn(fbToken)
            } else {
                appoxeeAdapter.optOut(fbToken)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "updateOptState() - failed to update opt state: $e")
            false
        }
    }

    private suspend fun loadCustomAttributesFromBackend(result: MutableMap<String, Any?>) {
        val attributesWithNoValue = result.filter { it.value == null }
        if (attributesWithNoValue.isNotEmpty()) {
            val response =
                appoxeeAdapter.getCustomAttributes(attributesWithNoValue.keys.toList())
            if (response.isSuccess()) {
                val payload = response.data?.payload
                if (!payload.isNullOrEmpty()) {
                    val updatedCache = storage.getCustomAttributesCache().attributes.toMutableMap()
                    payload.forEach { (key, value) ->
                        val normalized =
                            if (value?.toString()?.isNotEmpty() == true) value else null
                        result[key] = normalized
                        if (normalized == null) {
                            updatedCache.remove(key)
                        } else {
                            updatedCache[key] = normalized
                        }
                    }
                    storage.setCustomAttributesCache(updatedCache)
                }
            }
        }
    }

    override fun getCustomAttributes(attributes: Set<String>): Call<Map<String, Any?>> =
        buildHttpCall {
            val cachedAttributes = storage.getCustomAttributesCache().attributes
            val result =
                attributes.associateWith { cachedAttributes.getOrElse(it) { null } }.toMutableMap()
            loadCustomAttributesFromBackend(result)
            result
        }

    override fun removeCustomAttributes(attributes: Set<String>): Call<Boolean> = buildHttpCall {
        val cachedAttributes = storage.getCustomAttributesCache().attributes
        val attributesToUpdate = attributes.filter { cachedAttributes.keys.contains(it) }.toSet()
        if (attributesToUpdate.isNotEmpty()) {
            // on backend we can not delete attributes, but we are setting theirs value to empty string
            val response =
                appoxeeAdapter.addCustomAttributes(attributesToUpdate.associateWith { "" })
            if (response.isSuccess()) {
                storage.removeCustomAttributes(attributesToUpdate)
            }
        }
        true
    }

    override fun getDevice(): Call<DevicePayload?> = buildHttpCall {
        appoxeeAdapter.getDevice()
    }


    override suspend fun updateReadyStatus(status: Boolean, mappResult: MappResult<DevicePayload>) {
        mIsReady.set(status)
        observersProvider.notify(status, mappResult)
        internalScope.launch(dispatcherProvider.defaultDispatcher) {
            while (true) {
                val msg = pushQueue.poll() ?: break
                pushContainer.pushManager.handlePushMessage(application.applicationContext, msg)
            }
        }
    }

    override fun subscribe(observer: AppoxeeObserver) {
        internalScope.launch(dispatcherProvider.defaultDispatcher) {
            mutex.withLock {
                val payload = storage.getDevicePayload()
                observersProvider.addObserver(observer)
                if (isReady()) {
                    val result =
                        if (payload != null)
                            MappResult.Success(payload)
                        else
                            MappResult.Error(
                                Throwable("Invalid initialization!\nEngage SDK wasn't supplied with initialization parameters!")
                            )
                    withContext(dispatcherProvider.mainDispatcher) {
                        observersProvider.notify(true, result)
                    }
                }
            }
        }
    }

    override fun unsubscribe(observer: AppoxeeObserver) {
        observersProvider.removeObserver(observer)
    }

    override fun handlePushMessage(remoteMessage: RemoteMessage) {
        internalScope.launch {
            if (isReady()) {
                pushContainer.pushManager.handlePushMessage(
                    application.applicationContext,
                    remoteMessage
                )
            } else {
                pushQueue.add(remoteMessage)
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun addToQueue(remoteMessage: RemoteMessage) {
        pushQueue.add(remoteMessage)
    }

    override fun isPushMessageFromMapp(remoteMessage: RemoteMessage): Boolean {
        return pushContainer.pushManager.isPushMessageFromMapp(remoteMessage)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal suspend fun fetchAppConfig() {
        withContext(dispatcherProvider.defaultDispatcher) {
            if (!storage.isCacheValid()) {
                val result = appoxeeAdapter.getAppConfig()
                if (result.isSuccess()) {
                    storage.saveAppConfig(result.data?.payload)
                    storage.updateCacheTimestamp()
                    Logger.d(TAG, "APP CONFIG: ${result.data?.toString()}")
                } else {
                    Logger.e(TAG, result.error?.toString() ?: "Error")
                }
            }
        }
    }

    override fun closeNotification(notificationId: Int) {
        pushContainer.pushManager.dismissNotification(notificationId = notificationId)
    }

    override fun <T : LocalPushBroadcast> setPushBroadcast(clazz: Class<T>) {
        internalScope.launch {
            val requiredClass = LocalPushBroadcast::class.java
            if (requiredClass.isAssignableFrom(clazz)) {
                appoxeeContainer.localPushBroadcast = clazz
                withContext(dispatcherProvider.defaultDispatcher) {
                    storage.setBroadcastClass(clazz)
                }
            } else {
                throw IllegalArgumentException("PushBroadcast must be subtype of LocalPushBroadcast")
            }
        }
    }

    override fun updateFirebaseToken(token: String): Call<Boolean> = buildHttpCall {
        val device = storage.getDevicePayload() ?: appoxeeAdapter.getDevice() ?: return@buildHttpCall false
        if (device.pushToken?.isNotEmpty() == true) {
            appoxeeAdapter.optIn(token)
        } else {
            appoxeeAdapter.optOut(token)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun <T> buildHttpCall(
        call: suspend () -> T
    ): Call<T> {
        return HttpCall(
            scope = internalScope,
            call = call,
            dispatchersProvider = dispatcherProvider
        )
    }
}
