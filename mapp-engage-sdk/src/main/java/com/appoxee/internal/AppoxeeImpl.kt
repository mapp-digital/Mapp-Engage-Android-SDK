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
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("UNCHECKED_CAST")
@Keep
internal open class AppoxeeImpl(
    private val application: Application,
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

    private val mIsReady by lazy { AtomicBoolean(false) }

    private val pushQueue by lazy { mutableSetOf<RemoteMessage>() }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val internalScope =
        CoroutineScope(SupervisorJob() + CoroutineExceptionHandler { coroutineContext, throwable ->
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
        internalScope.launch(dispatcherProvider.defaultDispatcher) {
            // initialize logger
            Logger.init(application)

            // attach activity lifecycle listener
            application.registerActivityLifecycleCallbacks(
                appoxeeContainer.activityLifecycleHandler
            )

            // initialize sdk
            initializeSdk()

            //init webview
//            withContext(dispatchersProvider.mainDispatcher) {
//                MappWebView.getInstance(application)
//            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal suspend fun initializeSdk() {
        println("OPTIONS: $options")
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
            println("has device payload")
            // check if saved registration data and currently calculated registration data differs
            if(savedRegisterPayload!=null){

                println("has registration payload")
                val updatedParams=savedRegisterPayload.getChangedParams(newRegisterPayload)

                if(updatedParams.isNotEmpty()){
                    appoxeeAdapter.updateDevice(updatedParams)
                }
                // device already registered and channel is unchanged
                updateOptStatus(devicePayload, null)
            } else  {
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
        appoxeeAdapter.setAlias(alias)?.dmcUserId
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


    override suspend fun updateReadyStatus(status: Boolean, mappResult: MappResult<DevicePayload>) =
        withContext(dispatcherProvider.mainDispatcher) {
            mIsReady.set(status)
            observersProvider.notify(status, mappResult)
            internalScope.launch {
                pushQueue.forEach {
                    pushContainer.pushManager.handlePushMessage(application.applicationContext, it)
                }
            }
            Unit
        }

    override fun subscribe(observer: AppoxeeObserver) {
        internalScope.launch {
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
            if (clazz.superclass == requiredClass) {
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
        val device = appoxeeAdapter.getDevice() ?: return@buildHttpCall false
        if (device.pushToken?.isNotEmpty() == true && token.equals(device.pushToken, false)) {
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