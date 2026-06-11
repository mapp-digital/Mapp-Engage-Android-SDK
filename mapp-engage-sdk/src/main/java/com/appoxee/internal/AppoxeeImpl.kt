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
import com.appoxee.internal.model.request.RegisterDevice
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.inbox.InboxMessageDto
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse
import com.appoxee.internal.model.response.inbox.toDto
import com.appoxee.internal.model.response.inbox.toPublic
import com.appoxee.shared.InboxMessage
import com.appoxee.shared.InboxMessagesResponse as PublicInboxMessagesResponse
import com.appoxee.shared.MessageStatus
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

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

    private val registrationTimestampMs = AtomicLong(0L)

    private companion object {
        const val POST_REGISTRATION_DELAY_MS = 2_000L
        const val POST_REGISTRATION_MAX_RETRIES = 3
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val pushQueue by lazy { ConcurrentLinkedQueue<RemoteMessage>() }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val internalScope =
        CoroutineScope(SupervisorJob() + dispatcherProvider.mainDispatcher + CoroutineExceptionHandler { coroutineContext, throwable ->
            Logger.e(this.javaClass.name, "exception in sdk init: $throwable")
            observersProvider.notify(isReady(), MappResult.Error(throwable))
        })

    private val actionContainer: ActionContainer by lazy { ActionContainer(application) }

    private val inappContainer: InAppContainer by lazy {
        InAppContainer(
            appoxeeContainer.statsClient,
            actionContainer
        )
    }

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
            if (!options.areEquals(storage.getInitOptions())) {
                storage.clearRegistration()
            }
            // always store options for possible changes of other attributes, not used for comparing
            storage.saveInitOptions(options)
        } else {
            checkNotNull(storage.getInitOptions()) { "Engage SDK wasn't supplied with initialization parameters!" }
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
        val devicePayload: DevicePayload? = storage.getDevicePayload()

        // get cached registration data used to register device on server
        val savedRegisterPayload = storage.getRegistrationDevice()

        // calculate current registration data of device
        val newRegisterPayload = deviceProvider.generateRegistrationDevice()

        val validatedDevicePayload =
            if (devicePayload?.udidHashed != null) {
                validateExistingRegistration(
                    devicePayload = devicePayload,
                    savedRegisterPayload = savedRegisterPayload,
                    newRegisterPayload = newRegisterPayload
                )
            } else {
                validateMissingRegistration(devicePayload, newRegisterPayload)
            }

        saveValidatedRegistration(newRegisterPayload, validatedDevicePayload)

        // returns device payload
        return validatedDevicePayload
    }

    private suspend fun validateExistingRegistration(
        devicePayload: DevicePayload,
        savedRegisterPayload: RegisterDevice?,
        newRegisterPayload: RegisterDevice
    ): DevicePayload? {
        // if local device payload exist and data are not expired
        Logger.d(TAG, "has device payload")
        // check if saved registration data and currently calculated registration data differs
        return if (savedRegisterPayload != null) {
            Logger.d(TAG, "has registration payload")
            val updatedParams = savedRegisterPayload.getChangedParams(newRegisterPayload)
            val alias = devicePayload.alias

            if (!alias.isNullOrEmpty() && updatedParams.isNotEmpty()) {
                appoxeeAdapter.updateDevice(alias = alias, params = updatedParams)
            }

            // device already registered and channel is unchanged
            updateOptStatus(devicePayload, null)
            devicePayload
        } else {
            registerChangedRegistration(devicePayload, newRegisterPayload)
        }
    }

    private suspend fun registerChangedRegistration(
        devicePayload: DevicePayload?,
        newRegisterPayload: RegisterDevice
    ): DevicePayload? {
        // when registration data differs, register data again to update values on server
        appoxeeAdapter.register(newRegisterPayload)

        // update optIn or optOut status with firebase token
        // this fulfills requirement to preserve OptIn/OptOut state when channel changed
        updateOptStatus(devicePayload, null)

        // get device payload from server after new registration
        Logger.d(TAG, "validateRegistration - savedRegistration != newRegistration")
        return appoxeeAdapter.getDevice()
    }

    private suspend fun validateMissingRegistration(
        currentDevicePayload: DevicePayload?,
        newRegisterPayload: RegisterDevice
    ): DevicePayload? {
        // check if device registered with older version (v6) and needs to be migrated
        val oldOptions = migrationHelper.getRegistrationOptions()

        // get old registration options
        val oldRegistration = migrationHelper.fetchRegistrationData()

        // if channel is unchanged, reuse device registration and do not register device again.
        // migrate registration to SDK v7 structure and delete SDK v6 registration data.
        val devicePayload =
            if (options?.areEquals(oldOptions) == true) {
                migrateSameChannelRegistration(oldRegistration)
            } else {
                currentDevicePayload
            }

        // Device payload is null if device was not previously registered or if channel data were changed.
        // In this case, register new device and update optIn/optOut state
        return if (devicePayload?.udidHashed != null) {
            devicePayload
        } else {
            registerNewDevice(newRegisterPayload, devicePayload, oldRegistration)
        }
    }

    private suspend fun migrateSameChannelRegistration(
        oldRegistration: OldRegistration?
    ): DevicePayload? {
        val devicePayload = appoxeeAdapter.getDevice()

        // update only optOut state to refresh firebase token if it is changed
        updateOptStatus(devicePayload, oldRegistration)

        // delete old registration data only if migration succeeded;
        // if getDevice() failed (e.g. network error), keep v6 data so the
        // migration can be retried on the next launch instead of re-registering.
        if (devicePayload.hasValidUdid()) {
            seedMigratedRegistration(oldRegistration)
            migrationHelper.deleteOldRegistration()
        }

        return devicePayload
    }

    private suspend fun seedMigratedRegistration(oldRegistration: OldRegistration?) {
        // seed v7 local cache with tags and custom attributes from v6
        // so they are available without requiring a full re-sync from the server
        oldRegistration?.let { reg ->
            if (reg.tags.isNotEmpty()) storage.addTags(reg.tags.toList())
            if (reg.customAttributes.isNotEmpty()) storage.setCustomAttributesCache(reg.customAttributes)
        }
    }

    private suspend fun registerNewDevice(
        newRegisterPayload: RegisterDevice,
        devicePayload: DevicePayload?,
        oldRegistration: OldRegistration?
    ): DevicePayload? {
        // if device payload doesn't exist after all checkins, register device
        val registerPayload = appoxeeAdapter.register(newRegisterPayload)

        if (registerPayload != null) {
            registrationTimestampMs.set(System.currentTimeMillis())
        }

        // update optIn or optOut status with firebase token
        updateOptStatus(devicePayload, oldRegistration)

        // delete old registration data if still exists
        migrationHelper.deleteOldRegistration()

        // get device payload from server after new registration
        Logger.d(TAG, "validateRegistration - new device registered; udidHashed != null")
        return appoxeeAdapter.getDevice()
    }

    private suspend fun saveValidatedRegistration(
        newRegisterPayload: RegisterDevice,
        devicePayload: DevicePayload?
    ) {
        // save device registration data (device fingerprint) to a local storage for later access
        storage.saveRegistrationDevice(newRegisterPayload)

        // save device payload from server for a registered device
        storage.saveDevicePayload(devicePayload)
    }

    private fun DevicePayload?.hasValidUdid(): Boolean = this?.udidHashed != null

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
        Logger.d(TAG, "PUSH TOKEN: $pushToken")

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

    override fun fetchInboxMessages(): Call<PublicInboxMessagesResponse?> = buildHttpCall {
        appoxeeAdapter.fetchInboxMessages("app_inbox")?.toPublic()
    }

    override fun fetchInboxMessage(templateId: Long): Call<InboxMessage?> =
        buildHttpCall {
            val response = appoxeeAdapter.fetchInboxMessages("app_inbox")
            response?.messages?.firstOrNull { it.templateId == templateId }?.toPublic()
        }


    override fun fetchLatestInboxMessage(): Call<InboxMessage?> =
        buildHttpCall {
            val response = appoxeeAdapter.fetchInboxMessages("app_inbox")
            response?.messages?.maxByOrNull { it.templateId }?.toPublic()
        }

    override fun updateInboxMessageStatus(
        message: InboxMessage,
        status: MessageStatus
    ): Call<Boolean> = buildHttpCall {
        val dto = InboxMessageDto(
            templateId = message.templateId,
            content = message.content,
            subject = message.subject,
            summary = message.summary,
            iconUrl = message.iconUrl,
            sentDate = message.sentDate,
            expireDate = message.expireDate,
            firstSentTs = message.firstSentTs,
            status = status.toDto(),
            isNativeInApp = message.isNativeInApp,
            extras = message.extras,
            eventId = message.eventId,
            eventKey = message.eventKey,
        )
        inappContainer.inappManager.markInboxMessageStatus(dto, status.toDto())
    }

    override fun showInboxMessage(context: Activity, message: InboxMessage) {
        val dto = InboxMessageDto(
            templateId = message.templateId,
            content = message.content,
            subject = message.subject,
            summary = message.summary,
            iconUrl = message.iconUrl,
            sentDate = message.sentDate,
            expireDate = message.expireDate,
            firstSentTs = message.firstSentTs,
            status = message.status.toDto(),
            isNativeInApp = message.isNativeInApp,
            extras = message.extras,
            eventId = message.eventId,
            eventKey = message.eventKey,
        )
        inappContainer.inappManager.showMessage(
            activity = context,
            message = dto.getInappMessage()
        )
    }

    override fun triggerInApp(context: Activity, eventName: String): Call<Boolean> = buildHttpCall {
        val registeredAt = registrationTimestampMs.get()
        val inappResponse = if (registeredAt > 0L) {
            var response = appoxeeAdapter.fetchInappMessages(eventName)
            var attempt = 1
            while (attempt <= POST_REGISTRATION_MAX_RETRIES) {
                val hasMessages = response?.webMessages?.isNotEmpty() == true ||
                        response?.nativeMessages?.isNotEmpty() == true
                if (hasMessages) {
                    Logger.d(TAG, "triggerInApp: got messages on attempt $attempt, skipping further retries")
                    break
                }
                Logger.d(TAG, "triggerInApp: attempt $attempt - no messages, waiting ${POST_REGISTRATION_DELAY_MS}ms before retry")
                delay(POST_REGISTRATION_DELAY_MS)
                if (attempt < POST_REGISTRATION_MAX_RETRIES) {
                    response = appoxeeAdapter.fetchInappMessages(eventName)
                }
                attempt++
            }
            response
        } else {
            appoxeeAdapter.fetchInappMessages(eventName)
        }
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

        isWorkerActive
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
            token?.trim()
                .orEmpty()
                .ifEmpty { FirebaseMessaging.getInstance().token.await() }
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
        val missingKeys = result.filter { it.value == null }.keys

        if (missingKeys.isNotEmpty()) {
            val response = appoxeeAdapter.getCustomAttributes(missingKeys.toList())
            if (response.isSuccess()) {
                handleAttributesResponse(response.data?.payload, result)
            }
        }
    }

    private suspend fun handleAttributesResponse(
        payload: Map<String, Any?>?,
        result: MutableMap<String, Any?>
    ) {
        if (payload.isNullOrEmpty()) return

        val updatedCache = storage.getCustomAttributesCache().attributes.toMutableMap()

        payload.forEach { (key, value) ->
            val normalized = if (!value?.toString().isNullOrEmpty()) value else null
            result[key] = normalized

            if (normalized == null) {
                updatedCache.remove(key)
            } else {
                updatedCache[key] = normalized
            }
        }

        storage.setCustomAttributesCache(updatedCache)
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
        val device =
            storage.getDevicePayload() ?: appoxeeAdapter.getDevice() ?: return@buildHttpCall false
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
