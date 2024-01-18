@file:Suppress("PrivatePropertyName")

package com.appoxee.internal

import android.app.Application
import android.content.Context
import com.appoxee.Appoxee
import com.appoxee.internal.container.AppoxeeContainer
import com.appoxee.internal.container.PushContainer
import com.appoxee.internal.container.StorageContainer
import com.appoxee.internal.model.request.events.EventType
import com.appoxee.internal.model.request.events.ClickType
import com.appoxee.internal.model.request.events.TrackingKey
import com.appoxee.internal.model.request.geo.GeoEvent
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.geo.RegionsResponse
import com.appoxee.internal.model.response.inapp.InappResponse
import com.appoxee.internal.model.response.inbox.InboxMessagesResponse
import com.appoxee.internal.network.Call
import com.appoxee.internal.network.HttpCall
import com.appoxee.internal.provider.DeviceProvider
import com.appoxee.internal.push.model.PushData.Companion.toPushData
import com.appoxee.internal.storage.Storage
import com.appoxee.internal.ui.ActivityLifecycleHandler
import com.appoxee.internal.ui.custom.MappWebView
import com.appoxee.internal.util.Logger
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.MappResult
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

internal class AppoxeeImpl(
    context: Context,
    options: AppoxeeOptions? = null,
) : Appoxee, AppoxeeObservable {

    private val TAG = AppoxeeImpl::class.java.name

    private val mutex = Mutex()

    private val observers: MutableSet<AppoxeeObserver> = mutableSetOf()

    private val mIsReady = AtomicBoolean(false)

    private val pushQueue = mutableSetOf<RemoteMessage>()

    private val storageContainer: StorageContainer by lazy {
        StorageContainer.getInstance(context)
    }

    private val appoxeeContainer by lazy {
        AppoxeeContainer(
            context = context,
            storage = storage
        )
    }
    internal val appoxeeAdapter: AppoxeeAdapter
        get() = appoxeeContainer.appoxeeAdapter
    internal val storage: Storage
        get() = storageContainer.storage
    internal val deviceProvider: DeviceProvider
        get() = appoxeeContainer.deviceProvider

    internal val callCoroutineContext
        get() = appoxeeContainer.baseScope

    internal val pushContainer: PushContainer by lazy { PushContainer(context) }

    private val internalCoroutineContext by lazy { CoroutineScope(Dispatchers.IO) }

    internal val activityLifecycleCallback =
        ActivityLifecycleHandler(context.applicationContext)

    init {
        Logger.init(context.applicationContext as Application)
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(
            activityLifecycleCallback
        )
        println("OPTIONS: $options")
        internalCoroutineContext.launch(CoroutineExceptionHandler { coroutineContext, throwable ->
            Logger.e(TAG, "exception in sdk init: $throwable")
        }) {
            // save config to local storage if not null
            options?.let {
                storage.saveInitOptions(it)
            }

            // check device registration
            // update if exist or register new device
            validateRegistration()?.let {
                withContext(Dispatchers.Main) {
                    updateReadyStatus(true, MappResult.Success(it))
                }
            }

            // when registration is validated
            // update optIn or optOut status with firebase token
            updateOptStatus()

            // fetch InApp Configuration parameters
            getAppConfig()

            //init webview
            withContext(Dispatchers.Main) {
                MappWebView.getInstance(context.applicationContext)
            }
        }
    }

    private suspend fun validateRegistration(): DevicePayload? {
        // get saved device from local storage
        var devicePayload: DevicePayload? = storage.getDevicePayload()

        // get registration data used to register device on server
        val savedRegisterPayload = storage.getRegistrationDevice()

        // calculate current registration data of device
        val newRegisterPayload = deviceProvider.generateRegistrationDevice()

        // if local device payload exist and data are not expired
        if (devicePayload?.udidHashed != null /* && not expired */) {
            // check if saved registration data and currently calculated registration data differs
            if (savedRegisterPayload != newRegisterPayload) {
                // when registration data differs, register data again to update valus on server
                appoxeeAdapter.register(newRegisterPayload)

                // get device payload from server after new registration
                devicePayload = appoxeeAdapter.getDevice()
            }
        } else {
            // device payload doesn't exist or expired
            // get new device payload from server
            devicePayload = safeCall { appoxeeAdapter.getDevice() }.getData()

            // check if device payload from server exist or not
            if (devicePayload?.udidHashed == null) {
                // if device payload doesn't exist on server, register device
                appoxeeAdapter.register(newRegisterPayload)

                // get device payload from server after new registration
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
        var devicePayload = storage.getDevicePayload()
        val pushToken = FirebaseMessaging.getInstance().token.await()
        Logger.d(TAG, "PUSH TOKEN: $pushToken")
        var modified: Boolean = false
        // if device opted Out and optOut token is expired, update optOut token
        if (devicePayload?.pushTokenBk?.isNotEmpty() == true
            && pushToken != devicePayload.pushTokenBk
        ) {
            appoxeeAdapter.optOut(pushToken)
            modified = true
        }

        // if device opted In and optIn token is expired, update optIn token
        if (devicePayload?.pushToken?.isNotEmpty() == true && pushToken != devicePayload.pushToken) {
            appoxeeAdapter.optIn(pushToken)
            modified = true
        }

        if (modified) {
            // get fresh device data from server
            devicePayload = appoxeeAdapter.getDevice()
            storage.saveDevicePayload(devicePayload)
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

    override fun fetchInboxMessages(eventName: String): Call<InboxMessagesResponse?> =
        buildHttpCall {
            appoxeeAdapter.fetchInboxMessages(eventName)
        }

    override fun fetchInappMessages(eventName: String): Call<InappResponse?> = buildHttpCall {
        appoxeeAdapter.fetchInappMessages(eventName)
    }

    override fun enablePush(enabled: Boolean): Call<Boolean> = buildHttpCall {
        val token = FirebaseMessaging.getInstance().token.await()
        if (enabled) {
            appoxeeAdapter.optIn(token)
        } else {
            appoxeeAdapter.optOut(token)
        }
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
            pushContainer.pushManager.handlePushMessage(it)
        }
    }

    override fun subscribe(observer: AppoxeeObserver) {
        internalCoroutineContext.launch {
            val payload = storage.getDevicePayload()
            payload?.let {
                withContext(Dispatchers.Main) {
                    observer.onReadyStatusChanged(
                        isReady(),
                        MappResult.Success(data = it)
                    )
                }
            }
            observers.add(observer)
        }
    }

    override fun unsubscribe(observer: AppoxeeObserver) {
        observers.remove(observer)
    }

    override fun handlePushMessage(remoteMessage: RemoteMessage) {
        internalCoroutineContext.launch {
            mutex.withLock {
                if (mIsReady.get()) {
                    withContext(Dispatchers.Main) {
                        pushContainer.pushManager.handlePushMessage(remoteMessage)
                    }
                } else {
                    pushQueue.add(remoteMessage)
                }
            }
        }
    }

    override fun isPushMessageFromMapp(remoteMessage: RemoteMessage): Boolean {
        return pushContainer.pushManager.isPushMessageFromMapp(remoteMessage.toPushData())
    }

    override fun testCall(): Call<String> = buildHttpCall {
        val start = System.currentTimeMillis()
        delay(5000)
        return@buildHttpCall "Response from testCall after delay of ${System.currentTimeMillis() - start} ms."
    }

    override fun testActivate(): Call<Boolean> = buildHttpCall {
        appoxeeAdapter.activate(3000).isSuccess()
    }

    override fun testInappEvent(): Call<Boolean> = buildHttpCall {
        val response = appoxeeAdapter.inappEvent(
            originalEventId = "b3852abb-e519-47fd-96da-babc8a3a7cd4",
            templateId = 124640L,
            trackingKey = TrackingKey.IA_MSG_DISPLAYED
        )
        response.isSuccess()
    }

    override fun testPushEvent(): Call<Boolean> = buildHttpCall {
        val response = appoxeeAdapter.pushEvent(
            124852,
            233861,
            ClickType.OPEN_DIALER,
            EventType.CLICK
        )
        response.isSuccess()
    }

    override fun testGetRegions(
        lat: Double,
        lng: Double,
        version: Int,
        pageSize: Int
    ): Call<RegionsResponse> = buildHttpCall {
        appoxeeAdapter.getRegions(lat, lng, version, pageSize).data?.payload ?: RegionsResponse(
            0,
            emptyList()
        )
    }

    override fun testRegionEvent(
        geoEvent: GeoEvent,
        latitude: Double,
        longitude: Double,
        regionId: Long,
        version: Int
    ): Call<Boolean> = buildHttpCall {
        appoxeeAdapter.eventRegions(geoEvent, latitude, longitude, regionId, version).isSuccess()
    }

    private fun saveConfiguration(options: AppoxeeOptions) = internalCoroutineContext.launch {
        storage.saveInitOptions(options)
    }

    private suspend fun getAppConfig() {
        val result = appoxeeAdapter.getAppConfig()
        if (result.isSuccess()) {
            storage.saveAppConfig(result.data?.payload)
            Logger.d(TAG, "APP CONFIG: ${result.data?.toString()}")
        } else {
            Logger.e(TAG, result.error?.toString() ?: "Error")
        }
    }

    override fun closeNotification(notificationId: Int) {
        pushContainer.pushManager.dismissNotification(notificationId = notificationId)
    }

    private fun <T> buildHttpCall(
        call: suspend () -> T
    ): Call<T> {
        return HttpCall(coroutineScope = callCoroutineContext, call)
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