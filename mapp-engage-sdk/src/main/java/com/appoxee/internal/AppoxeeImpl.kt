@file:Suppress("PrivatePropertyName")

package com.appoxee.internal

import android.app.Activity
import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.work.Data
import com.appoxee.Appoxee
import com.appoxee.internal.container.ActionContainer
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.container.InAppContainer
import com.appoxee.internal.container.PushContainer
import com.appoxee.internal.container.StatsContainer
import com.appoxee.internal.container.StorageContainer
import com.appoxee.internal.geo.GeofenceException
import com.appoxee.internal.model.request.geo.GeoEvent
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.geo.RegionsResponse
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inbox.InboxMessage
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse
import com.appoxee.internal.model.response.inbox.MessageStatus
import com.appoxee.internal.network.Call
import com.appoxee.internal.network.HttpCall
import com.appoxee.internal.storage.Storage
import com.appoxee.internal.ui.ActivityLifecycleHandler
import com.appoxee.internal.ui.custom.MappWebView
import com.appoxee.internal.util.Logger
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.GeoStatus
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

internal class AppoxeeImpl(
    private val application: Application,
    private val options: AppoxeeOptions? = null,
    private val dispatchers: com.appoxee.internal.util.Dispatchers,
) : Appoxee, AppoxeeObservable {

    private val TAG = AppoxeeImpl::class.java.name

    private val mutex = Mutex()

    private val internalScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + CoroutineExceptionHandler { coroutineContext, throwable ->
            Logger.e(TAG, "exception in sdk init: $throwable")
        })

    private val observers: MutableSet<AppoxeeObserver> by lazy { mutableSetOf() }

    private val mIsReady by lazy { AtomicBoolean(false) }

    private val pushQueue = mutableSetOf<RemoteMessage>()

    private val storageContainer: StorageContainer by lazy {
        StorageContainer.getInstance(application.applicationContext)
    }

    private val actionContainer: ActionContainer
        get() = ActionContainer(application)

    private val statsContainer: StatsContainer
        get() = StatsContainer(application.applicationContext, dispatchers)


    private val inappContainer: InAppContainer
        get() = InAppContainer(internalScope, statsContainer, actionContainer)


    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val appoxeeContainer = AppoxeeContainer.getInstance(
        context = application.applicationContext, storage = storage, dispatchers = dispatchers
    )

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val appoxeeAdapter: AppoxeeAdapter
        get() = appoxeeContainer.appoxeeAdapter

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val storage: Storage
        get() = storageContainer.storage

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val pushContainer: PushContainer
        get() = PushContainer(application.applicationContext)

    internal val activityLifecycleHandler: ActivityLifecycleHandler by lazy {
        ActivityLifecycleHandler(
            application,
            statsContainer.statsClient,
            appoxeeContainer.baseScope
        )
    }

    init {
        internalScope.launch(dispatchers.ioDispatcher) {
            Logger.init(application.applicationContext as Application)

            (application.applicationContext as Application).registerActivityLifecycleCallbacks(
                activityLifecycleHandler
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

            // when registration is validated
            // update optIn or optOut status with firebase token
            updateOptStatus()

            // fetch InApp Configuration parameters
            getAppConfig()

            //init webview
            withContext(dispatchers.mainDispatcher) {
                MappWebView.getInstance(application.applicationContext)
            }
        }
    }

    private suspend fun validateRegistration(): DevicePayload? {
        // get saved device from local storage
        var devicePayload: DevicePayload? = storage.getDevicePayload()

        // get registration data used to register device on server
        val savedRegisterPayload = storage.getRegistrationDevice()

        // calculate current registration data of device
        val newRegisterPayload = appoxeeContainer.deviceProvider.generateRegistrationDevice()

        // if local device payload exist and data are not expired
        if (devicePayload?.udidHashed != null /* && not expired */) {
            // check if saved registration data and currently calculated registration data differs
            if (savedRegisterPayload != newRegisterPayload) {
                // when registration data differs, register data again to update valus on server
                appoxeeAdapter.register(newRegisterPayload)

                // get device payload from server after new registration
                Logger.d(TAG, "validateRegistration - savedRegistration != newRegistration")
                devicePayload = appoxeeAdapter.getDevice()
            }
        } else {
            // cached device payload doesn't exist or expired
            // get new device payload from server
            Logger.d(TAG, "validateRegistration - cached udidHashed == null")
            devicePayload = appoxeeAdapter.getDevice()

            // check if device payload from server exist or not
            if (devicePayload?.udidHashed == null) {
                // if device payload doesn't exist on server, register device
                appoxeeAdapter.register(newRegisterPayload)

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


    private suspend fun updateOptStatus() {
        Logger.d(TAG, "updateOptStatus()")
        val devicePayload = storage.getDevicePayload()
        val pushToken = FirebaseMessaging.getInstance().token.await()
        Logger.d(TAG, "PUSH TOKEN: $pushToken")

        // if device opted Out and optOut token is expired, update optOut token
        if (devicePayload?.pushTokenBk?.isNotEmpty() == true && pushToken != devicePayload.pushTokenBk) {
            appoxeeAdapter.optOut(pushToken)
        }

        // if device opted In and optIn token is expired, update optIn token
        if (devicePayload?.pushToken?.isNotEmpty() == true && pushToken != devicePayload.pushToken) {
            appoxeeAdapter.optIn(pushToken)
        }

        Logger.d(TAG, "updateOptStatus() - Finished")
    }

    override fun isReady(): Boolean {
        return mIsReady.get()
    }

    override fun setAlias(alias: String): Call<String?> = buildHttpCall {
        appoxeeAdapter.setAlias(alias) ?: ""
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
        statsContainer.statsClient.markInboxMessageStatus(message, status)
    }

    override fun showInboxMessage(context: Activity, message: InboxMessage) {
        inappContainer.inappManager.showMessage(
            activity = context,
            message = message.getInappMessage()
        )
    }

    override fun fetchInappMessages(eventName: String): Call<InappResponse?> = buildHttpCall {
        val response = appoxeeAdapter.fetchInappMessages(eventName)
        response
    }

    override fun triggerInApp(context: Activity, eventName: String) {
        appoxeeContainer.baseScope.launch {
            val inappResponse = appoxeeAdapter.fetchInappMessages(eventName)
            inappContainer.inappManager.let { inappManager ->
                val sortedMessages = inappManager.parseResponse(inappResponse)
                withContext(dispatchers.mainDispatcher) {
                    inappManager.handleMessages(context, sortedMessages)
                }
            }
        }
    }

    override fun <T : GeoStatus> startGeofencing(enterDelaySeconds: Int): Call<T> = buildHttpCall {
        try {
            val data = Data.Builder().putInt("enterDelaySeconds", enterDelaySeconds).build()
            appoxeeContainer.geoContainer.locationUpdateScheduler.schedule(data = data)
            GeoStatus.GeoStartedOk()
        } catch (e: GeofenceException) {
            e.geoStatus
        } catch (e: Exception) {
            GeoStatus.GeoGeneralError()
        } as T
    }

    override fun <T : GeoStatus> stopGeofencing(): Call<T> = buildHttpCall {
        appoxeeContainer.geoContainer.locationUpdateScheduler.cancel()
        appoxeeContainer.geoContainer.geofencingClientWrapper.removeGeofences()
        GeoStatus.GeoStoppedOk() as T
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

    override fun testGetRegions(
        lat: Double, lng: Double, version: Int, pageSize: Int
    ): Call<RegionsResponse> = buildHttpCall {
        appoxeeAdapter.getRegions(lat, lng, version, pageSize).data?.payload ?: RegionsResponse(
            0, emptyList()
        )
    }

    override fun testRegionEvent(
        geoEvent: GeoEvent, latitude: Double, longitude: Double, regionId: Long, version: Int
    ): Call<Boolean> = buildHttpCall {
        appoxeeAdapter.eventRegions(geoEvent, latitude, longitude, regionId, version).isSuccess()
    }

    private fun saveConfiguration(options: AppoxeeOptions) {
        internalScope.launch {
            storage.saveInitOptions(options)
        }
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
                throw IllegalArgumentException("PushBroadcast must be of type LocalPushBroadcast")
            }
        }
    }

    private fun <T> buildHttpCall(
        call: suspend () -> T
    ): Call<T> {
        return HttpCall(coroutineScope = appoxeeContainer.baseScope, call)
    }

    private suspend fun <T> safeCall(
        call: suspend () -> T?
    ): MappResult<T?> {
        return try {
            val data = call.invoke()
            MappResult.Success(data)
        } catch (e: Exception) {
            Logger.e(TAG, "Appoxee call $call error: $e")
            MappResult.Error(e)
        }
    }
}