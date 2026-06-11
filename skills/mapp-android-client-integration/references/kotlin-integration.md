# Kotlin Integration

Use this path when the target module is Kotlin-first (`.kt` codebase or Kotlin Android plugin).

## Correct Imports

Always use these exact package paths. Never guess or invent alternatives.

```kotlin
import com.appoxee.Appoxee
import com.appoxee.shared.AppoxeeObserver
import com.appoxee.shared.AppoxeeOptions
import com.appoxee.shared.NotificationMode
import com.appoxee.shared.MappResult
import com.appoxee.shared.MappCallback
import com.appoxee.shared.MappPush
import com.appoxee.shared.LocalPushBroadcast
import com.appoxee.shared.ActionButton
import com.appoxee.shared.MappMessagingService
import com.appoxee.shared.GeoStatus
import com.appoxee.shared.GeofenceException
import com.appoxee.internal.model.response.DevicePayload
import com.appoxee.internal.model.response.inbox.InboxMessage
import com.appoxee.internal.model.response.inbox.MessageStatus
import com.appoxee.internal.network.Call
```

## Gradle (Kotlin DSL)

```kotlin
// app/build.gradle.kts
import java.util.Properties

// Add google-services plugin if not already present — do NOT add kotlin.android,
// it is already applied (bundled with AGP / declared in the project's plugins block).
plugins {
    id("com.google.gms.google-services")
}

// Read Mapp credentials from local.properties (git-ignored — never hardcode these)
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    defaultConfig {
        buildConfigField("String", "MAPP_SDK_KEY",   "\"${localProperties["mapp.sdk.key"]}\"")
        buildConfigField("String", "MAPP_APP_ID",    "\"${localProperties["mapp.app.id"]}\"")
        buildConfigField("String", "MAPP_TENANT_ID", "\"${localProperties["mapp.tenant.id"]}\"")
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// New form for setting Kotlin JVM target — must match compileOptions Java version above
// Requires: import org.jetbrains.kotlin.gradle.dsl.JvmTarget
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation("com.mapp.sdk:engage-android:7.0.2")
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-messaging")
}
```

**`local.properties`** (each developer adds their own values — file must be git-ignored):
```properties
mapp.sdk.key=YOUR_SDK_KEY
mapp.app.id=YOUR_APP_ID
mapp.tenant.id=YOUR_TENANT_ID
```

Also add `google-services.json` (from Firebase Console) to the `app/` directory.

## Application Initialization

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val options = AppoxeeOptions(
            server   = AppoxeeOptions.Server.L3,        // hardcoded — not a secret
            sdkKey   = BuildConfig.MAPP_SDK_KEY,        // from local.properties
            appId    = BuildConfig.MAPP_APP_ID,
            tenantId = BuildConfig.MAPP_TENANT_ID
        ).also {
            it.notificationMode = NotificationMode.BACKGROUND_AND_FOREGROUND
        }

        Appoxee.engage(this, options)
    }
}
```

## Manifest Essentials

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Optional notification icon metadata:

```xml
<meta-data
    android:name="com.engage.mapp_notification_small_icon"
    android:resource="@drawable/your_notification_small_icon" />
<meta-data
    android:name="com.engage.mapp_notification_large_icon"
    android:resource="@drawable/your_notification_large_icon" />
<meta-data
    android:name="com.engage.mapp_notification_small_icon_color"
    android:resource="@color/your_accent_color" />
```

## Async Calls (Kotlin Preferred)

Prefer coroutines and `asSuspend()`:

```kotlin
lifecycleScope.launch {
    val result = Appoxee.instance().getDevice().asSuspend()
    if (result.isSuccess()) {
        val device = result.getData()
    } else {
        val error = result.getError()
    }
}
```

Do not call `enqueue()` or `execute()` on the same `Call` returned above.

## Custom FirebaseMessagingService (Only if Needed)

Use this only when the client already has a custom FCM pipeline.

```kotlin
class CustomFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Appoxee.instance().updateFirebaseToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (Appoxee.instance().isPushMessageFromMapp(remoteMessage)) {
            Appoxee.instance().handlePushMessage(remoteMessage)
        } else {
            // Handle non-Mapp pushes
        }
    }
}
```

When enabling push from custom service flow, pass token to `enablePush(true, token)` after token retrieval.

## Base Startup Flow (Kotlin)

Wire this into the entry `Activity`. All SDK calls in this section go inside `onSdkReady()`,
which is only reached after `AppoxeeObserver` confirms a successful registration.

Methods covered here:
- `subscribe` / `unsubscribe` — lifecycle attach/detach
- `setAlias` — identify the user
- `enablePush` — push opt-in / opt-out
- `triggerInApp` — fire an in-app event
- `getDevice` — read device/registration info

`setPushBroadcast` belongs in the `Application` class (see Application Initialization above).
`updateFirebaseToken`, `isPushMessageFromMapp`, `handlePushMessage` belong in a custom
`FirebaseMessagingService` (see Custom FirebaseMessagingService section below).

```kotlin
class MainActivity : AppCompatActivity() {

    // -------------------------------------------------------------------------
    // subscribe / unsubscribe — attach observer to lifecycle, not to onCreate
    // -------------------------------------------------------------------------
    private val sdkObserver = object : AppoxeeObserver {
        override fun onReadyStatusChanged(status: Boolean, result: MappResult<DevicePayload>) {
            if (!result.isSuccess()) return
            lifecycleScope.launch { onSdkReady() }
        }
    }

    override fun onStart() {
        super.onStart()
        Appoxee.instance().subscribe(sdkObserver)
    }

    override fun onStop() {
        super.onStop()
        Appoxee.instance().unsubscribe(sdkObserver)
    }

    // -------------------------------------------------------------------------
    // Called once the SDK is ready — gate all SDK calls behind this
    // -------------------------------------------------------------------------
    private suspend fun onSdkReady() {
        // setAlias — identify the user in Mapp
        val userId = getCurrentUserId()   // replace with app's auth source
        if (userId != null) {
            Appoxee.instance().setAlias(userId, resendCustomAttributes = true).asSuspend()
        }

        // enablePush — show consent dialog if the user hasn't decided yet
        if (!hasUserDecidedPushConsent()) {
            showPushConsentDialog()
        }

        // triggerInApp — fire session-start event
        Appoxee.instance().triggerInApp(this@MainActivity, "app_open").asSuspend()

        // getDevice — read current device / registration info
        loadDeviceInfo()
    }

    // -------------------------------------------------------------------------
    // enablePush — opt in or out based on user choice
    // -------------------------------------------------------------------------
    private fun showPushConsentDialog() {
        AlertDialog.Builder(this)
            .setTitle("Stay up to date")
            .setMessage("Allow push notifications to receive the latest offers and updates.")
            .setPositiveButton("Allow") { _, _ ->
                lifecycleScope.launch {
                    Appoxee.instance().enablePush(true).asSuspend()
                    savePushConsent(true)
                }
            }
            .setNegativeButton("No thanks") { _, _ ->
                lifecycleScope.launch {
                    Appoxee.instance().enablePush(false).asSuspend()
                    savePushConsent(false)
                }
            }
            .setCancelable(false)
            .show()
    }

    // -------------------------------------------------------------------------
    // getDevice — fetch device registration payload
    // -------------------------------------------------------------------------
    private suspend fun loadDeviceInfo() {
        val result = Appoxee.instance().getDevice().asSuspend()
        if (result.isSuccess()) {
            val device = result.getData()
            // use device?.deviceId, device?.alias, etc.
        } else {
            val error = result.getError()
            // log or show error
        }
    }

    // -------------------------------------------------------------------------
    // Implement these with SharedPreferences or your app's storage layer
    // -------------------------------------------------------------------------
    private fun getCurrentUserId(): String? = null
    private fun hasUserDecidedPushConsent(): Boolean = false
    private fun savePushConsent(accepted: Boolean) {}
}
```

Note: `enablePush(true/false)` controls the Mapp backend opt-in state. For Android 13+,
request `POST_NOTIFICATIONS` separately (e.g. just before showing this dialog).

## Kotlin-Specific Best Practices

- Keep SDK calls behind repository/use-case layers for testability.
- Keep blocking calls off main thread; prefer suspend wrappers.
- Attach observer subscription/unsubscription to lifecycle boundaries.
- Reuse existing coroutine scope strategy used in the app (for example `viewModelScope` or `lifecycleScope`).
