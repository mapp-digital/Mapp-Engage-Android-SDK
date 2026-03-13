# Kotlin Integration

Use this path when the target module is Kotlin-first (`.kt` codebase or Kotlin Android plugin).

## Gradle (Kotlin DSL)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

dependencies {
    implementation("com.mapp.sdk:engage-android:7.0.0-beta04")
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
}
```

## Application Initialization

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val options = AppoxeeOptions(
            server = AppoxeeOptions.Server.L3,
            sdkKey = "YOUR_SDK_KEY",
            appId = "YOUR_APP_ID",
            tenantId = "YOUR_TENANT_ID"
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

## Kotlin-Specific Best Practices

- Keep SDK calls behind repository/use-case layers for testability.
- Keep blocking calls off main thread; prefer suspend wrappers.
- Attach observer subscription/unsubscription to lifecycle boundaries.
- Reuse existing coroutine scope strategy used in the app (for example `viewModelScope` or `lifecycleScope`).
