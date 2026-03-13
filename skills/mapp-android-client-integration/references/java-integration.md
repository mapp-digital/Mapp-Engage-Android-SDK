# Java Integration (Legacy Apps)

Use this path when the app is legacy Java-first (`.java` codebase, Java architecture, callback style).

## Gradle (Groovy)

```groovy
plugins {
    id 'com.android.application'
    id 'com.google.gms.google-services'
}

dependencies {
    implementation 'com.mapp.sdk:engage-android:7.0.0-beta04'
    implementation platform('com.google.firebase:firebase-bom:34.6.0')
    implementation 'com.google.firebase:firebase-messaging-ktx'
}
```

## Application Initialization

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

## Manifest Essentials

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

If custom `FirebaseMessagingService` is used, remove SDK service entry:

```xml
<service
    android:name="com.appoxee.shared.MappMessagingService"
    tools:node="remove" />
```

## Async Calls (Java Preferred)

Prefer callback mode in UI flow:

```java
Appoxee.instance().getDevice().enqueue(result -> {
    if (result.isSuccess()) {
        DevicePayload data = result.getData();
    } else {
        Throwable error = result.getError();
    }
});
```

For synchronous needs, execute only on worker thread:

```java
Executors.newSingleThreadExecutor().execute(() -> {
    MappResult<DevicePayload> result = Appoxee.instance().getDevice().execute();
    // handle result
});
```

Do not consume the same `Call<T>` multiple times.

## Custom FirebaseMessagingService (Java)

```java
public class CustomFirebaseMessaging extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Appoxee.instance().updateFirebaseToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        if (Appoxee.instance().isPushMessageFromMapp(message)) {
            Appoxee.instance().handlePushMessage(message);
        } else {
            // Handle non-Mapp pushes
        }
    }
}
```

## Java-Specific Best Practices

- Keep Appoxee calls in dedicated manager/service classes to avoid activity bloat.
- Avoid anonymous callback duplication; extract reusable result handlers.
- Use explicit executor policy for all `execute()` usage.
- Keep legacy code style consistent with existing app conventions; avoid partial Kotlin migration unless requested.
