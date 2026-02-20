# Mapp Engage Android SDK

The Mapp Engage Android SDK enables push notifications, in-app messaging, geofencing, and user engagement features in your Android applications.

**Official documentation:** https://docs.mapp.com/v1/docs/android-sdk-1

---

## Requirements

| Requirement | Version |
|-------------|---------|
| **minSdk** | 23 |
| **compileSdk** | 36 |
| **targetSdk** | 36 |
| **Java** | 17 |
| **Kotlin** | 2.2.21+ |

### Dependencies

The SDK requires:

- **Firebase Cloud Messaging** – for push notifications
- **Google Play Services Location** – for geofencing
- **AndroidX** – AppCompat, Lifecycle, Coroutines
- **Firebase BOM** – 34.6.0 (or compatible)

---

## Installation

### Gradle (Kotlin DSL)

In your project-level `build.gradle.kts`:

```kotlin
buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")  // if using JitPack
    }
}

plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
}
```

In your app-level `build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    compileSdk = 36
    defaultConfig {
        minSdk = 23
        targetSdk = 36
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("com.mapp.sdk:engage-android:7.0.0-beta04")
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'com.mapp.sdk:engage-android:7.0.0-beta04'
    implementation platform('com.google.firebase:firebase-bom:34.6.0')
    implementation 'com.google.firebase:firebase-messaging-ktx'
}
```

---

## Configuration

### 1. Permissions

Add to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### 2. Firebase Messaging Service

The SDK includes `MappMessagingService` which is registered automatically. If you need a custom `FirebaseMessagingService`, see [Custom Push Handling](#custom-push-handling) below.

### 3. Notification Icons (Optional)

Add meta-data in your manifest for custom notification icons:

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

---

## Usage

### Initialization

Initialize the SDK in your `Application` class. **Must be called from the main thread.**

**Kotlin:**

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

**Java:**

```java
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppoxeeOptions options = new AppoxeeOptions(
                AppoxeeOptions.Server.L3,
                "YOUR_SDK_KEY",
                "YOUR_APP_ID",
                "YOUR_TENANT_ID"
        );
        options.setNotificationMode(NotificationMode.BACKGROUND_AND_FOREGROUND);
        Appoxee.engage(this, options);
    }
}
```

### Configuration Options

| Parameter | Description |
|-----------|-------------|
| `server` | `AppoxeeOptions.Server` – L3, L3_US, EMC, EMC_US, CROC, TEST, TEST_55, TEST_61 |
| `sdkKey` | SDK Key from Mapp Engage channel |
| `appId` | App ID from Mapp Engage channel |
| `tenantId` | Client's unique tenant identifier |
| `notificationMode` | `BACKGROUND_ONLY` or `BACKGROUND_AND_FOREGROUND` |

### Get SDK Instance

```kotlin
// Throws NullPointerException if engage() was not called first
val appoxee = Appoxee.instance()
```

### Async API Results – `Call<T>`

Most SDK methods (`getDevice()`, `setAlias()`, `enablePush()`, `addTags()`, etc.) return a `Call<T>` rather than the result directly. `Call<T>` is a deferred, single-use handle: it represents a network or background operation that has not yet run. You must invoke one of its methods to obtain the `MappResult<T>`.

**Important:**
- Each `Call` instance may be consumed **only once**. Calling more than one consumer method (e.g. both `asSuspend()` and `enqueue()` on the same `Call`) will throw `CallConsumedException`.
- Results are delivered as `MappResult<T>`, which can be either `MappResult.Success(data)` or `MappResult.Error(throwable)`. Use `isSuccess()`, `getData()`, and `getError()` to inspect the outcome.

| Method | Thread | Use case |
|--------|--------|----------|
| `asSuspend()` | Coroutine / suspend function | Kotlin coroutines; non-blocking, preferred in Kotlin |
| `enqueue(callback)` | Main thread to invoke | Callback-based; result delivered on main thread |
| `execute()` | Background thread (`@WorkerThread`) | Blocking; suitable for Java or synchronous code |

**Kotlin (Coroutines) – `asSuspend()`:**
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

**Callback – `enqueue()` (invoke from main thread):**
```kotlin
Appoxee.instance().getDevice().enqueue(object : MappCallback<DevicePayload?> {
    override fun onResult(result: MappResult<DevicePayload?>) {
        if (result.isSuccess()) {
            val device = result.getData()
        } else {
            result.getError()?.let { /* handle error */ }
        }
    }
})
```

**Blocking – `execute()` (only from a background thread):**
```kotlin
// From a background thread or Executor
val result = Appoxee.instance().getDevice().execute()
```

---

## Core Features

### Push Notifications

| Method | Description |
|--------|-------------|
| `enablePush(enabled: Boolean, token: String? = null)` | Opt in/out of push. Pass token only if using custom `FirebaseMessagingService`. |
| `isPushEnabled()` | Check if device is opted in |
| `getFirebaseToken()` | Get current Firebase token |
| `updateFirebaseToken(token: String)` | Update token for registered device |

### User Identity & Attributes

| Method | Description |
|--------|-------------|
| `setAlias(alias: String, resendCustomAttributes: Boolean = false)` | Set custom user alias |
| `getAlias()` | Get current alias |
| `addTags(tags: Set<String>)` | Add tags to device |
| `removeTags(tags: Set<String>)` | Remove tags |
| `getTags()` | Get tags from cache |
| `addCustomAttributes(attributes: Map<String, Any?>)` | Add custom attributes |
| `getCustomAttributes(attributes: Set<String>)` | Get specific attributes |
| `removeCustomAttributes(attributes: Set<String>)` | Remove attributes |

### Inbox Messages

| Method | Description |
|--------|-------------|
| `fetchInboxMessages()` | Get list of inbox messages |
| `fetchInboxMessage(templateId: Long)` | Get message by template ID |
| `fetchLatestInboxMessage()` | Get latest message |
| `updateInboxMessageStatus(message, status)` | Update message status |
| `showInboxMessage(context, message)` | Show message as banner/dialog/fullscreen |

### In-App Messages

| Method | Description |
|--------|-------------|
| `triggerInApp(context: Activity, eventName: String)` | Fetch and show in-app messages for event |

### Geofencing

| Method | Description |
|--------|-------------|
| `startGeofencing(enterDelaySeconds: Int = 0)` | Start geofence tracking. 0 = Enter event, else DWELL |
| `stopGeofencing()` | Stop geofence tracking |
| `isGeofencingActive()` | Check if geofencing is active |

### Other

| Method | Description |
|--------|-------------|
| `getDevice()` | Get registered device payload |
| `isReady()` | Check if SDK is ready |
| `logout(pushEnabled: Boolean)` | Log out user, reset alias |
| `closeNotification(notificationId: Int)` | Close notification by ID |

---

## Push Event Handling

### LocalPushBroadcast (Optional)

Register a broadcast receiver to handle push events:

```kotlin
// 1. Create your receiver extending LocalPushBroadcast
class MyPushBroadcast : LocalPushBroadcast() {
    override fun onReceived(push: MappPush) { /* Push received */ }
    override fun onOpened(push: MappPush) { /* User opened push */ }
    override fun onSilent(push: MappPush) { /* Silent push */ }
    override fun onDismissed(push: MappPush) { /* User dismissed */ }
    override fun onButtonClick(push: MappPush) { /* Action button clicked */ }
    override fun onRichPush(push: MappPush) { /* Rich push */ }
}

// 2. Register in Application or Activity
Appoxee.instance().setPushBroadcast(MyPushBroadcast::class.java)
```

Add the receiver to your manifest:

```xml
<receiver
    android:name=".MyPushBroadcast"
    android:enabled="true"
    android:exported="false" />
```

### AppoxeeObserver

Subscribe to SDK status updates:

```kotlin
val observer = object : AppoxeeObserver {
    override fun onReadyStatusChanged(status: Boolean, mappResult: MappResult<DevicePayload>) {
        if (!mappResult.isSuccess()) {
            val error = mappResult.getError()
        }
    }
}
Appoxee.instance().subscribe(observer)
// Later: Appoxee.instance().unsubscribe(observer)
```

---

## Custom Push Handling

If you use your own `FirebaseMessagingService` instead of `MappMessagingService`:

1. Disable `MappMessagingService` in your manifest (e.g. with `tools:node="remove"`).
2. In your service's `onMessageReceived`:

```kotlin
override fun onMessageReceived(remoteMessage: RemoteMessage) {
    if (Appoxee.instance().isPushMessageFromMapp(remoteMessage)) {
        Appoxee.instance().handlePushMessage(remoteMessage)
    } else {
        // Handle your own pushes
    }
}
```

3. When opting in, pass the Firebase token:

```kotlin
FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    task.result?.let { token ->
        Appoxee.instance().enablePush(true, token)
    }
}
```

---

## Sample Apps

- **sample-kotlin** – Kotlin example with Compose
- **sample-java** – Java example

Run a sample with the `prod` flavor for production server, or `tst` for test.

---

## License

MIT License. See [LICENSE.md](LICENSE.md).
