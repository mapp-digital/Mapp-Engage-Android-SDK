# Java Integration (Legacy Apps)

Use this path when the app is legacy Java-first (`.java` codebase, Java architecture, callback style).

## Correct Imports

Always use these exact package paths. Never guess or invent alternatives.

```java
import com.appoxee.Appoxee;
import com.appoxee.shared.AppoxeeObserver;
import com.appoxee.shared.AppoxeeOptions;
import com.appoxee.shared.NotificationMode;
import com.appoxee.shared.MappResult;
import com.appoxee.shared.MappCallback;
import com.appoxee.shared.MappPush;
import com.appoxee.shared.LocalPushBroadcast;
import com.appoxee.shared.ActionButton;
import com.appoxee.shared.MappMessagingService;
import com.appoxee.shared.GeoStatus;
import com.appoxee.shared.GeofenceException;
import com.appoxee.internal.model.response.DevicePayload;
import com.appoxee.internal.model.response.inbox.InboxMessage;
import com.appoxee.internal.model.response.inbox.MessageStatus;
import com.appoxee.internal.network.Call;
```

## Gradle (Groovy)

```groovy
// app/build.gradle
plugins {
    id 'com.android.application'
    id 'com.google.gms.google-services'
}

// Read Mapp credentials from local.properties (git-ignored — never hardcode these)
def localProperties = new Properties()
def localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.withReader("UTF-8") { reader -> localProperties.load(reader) }
}

android {
    defaultConfig {
        buildConfigField "String", "MAPP_SDK_KEY",   "\"${localProperties['mapp.sdk.key']}\""
        buildConfigField "String", "MAPP_APP_ID",    "\"${localProperties['mapp.app.id']}\""
        buildConfigField "String", "MAPP_TENANT_ID", "\"${localProperties['mapp.tenant.id']}\""
    }
    buildFeatures {
        buildConfig true
    }
}

dependencies {
    implementation 'com.mapp.sdk:engage-android:7.0.2'
    implementation platform('com.google.firebase:firebase-bom:34.11.0')
    implementation 'com.google.firebase:firebase-messaging'
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

```java
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        AppoxeeOptions options = new AppoxeeOptions(
                AppoxeeOptions.Server.L3,        // hardcoded — not a secret
                BuildConfig.MAPP_SDK_KEY,        // from local.properties
                BuildConfig.MAPP_APP_ID,
                BuildConfig.MAPP_TENANT_ID
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

## Base Startup Flow (Java)

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

```java
public class MainActivity extends AppCompatActivity {

    // -------------------------------------------------------------------------
    // subscribe / unsubscribe — attach observer to lifecycle, not to onCreate
    // -------------------------------------------------------------------------
    private final AppoxeeObserver sdkObserver = (status, result) -> {
        if (!result.isSuccess()) return;
        onSdkReady();
    };

    @Override
    protected void onStart() {
        super.onStart();
        Appoxee.instance().subscribe(sdkObserver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Appoxee.instance().unsubscribe(sdkObserver);
    }

    // -------------------------------------------------------------------------
    // Called once the SDK is ready — gate all SDK calls behind this
    // -------------------------------------------------------------------------
    private void onSdkReady() {
        // setAlias — identify the user in Mapp
        String userId = getCurrentUserId();   // replace with app's auth source
        if (userId != null) {
            Appoxee.instance().setAlias(userId, true).enqueue(result -> {});
        }

        // enablePush — show consent dialog if the user hasn't decided yet
        if (!hasUserDecidedPushConsent()) {
            showPushConsentDialog();
        }

        // triggerInApp — fire session-start event
        Appoxee.instance().triggerInApp(this, "app_open").enqueue(result -> {});

        // getDevice — read current device / registration info
        loadDeviceInfo();
    }

    // -------------------------------------------------------------------------
    // enablePush — opt in or out based on user choice
    // -------------------------------------------------------------------------
    private void showPushConsentDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Stay up to date")
            .setMessage("Allow push notifications to receive the latest offers and updates.")
            .setPositiveButton("Allow", (d, w) -> {
                Appoxee.instance().enablePush(true).enqueue(r -> {});
                savePushConsent(true);
            })
            .setNegativeButton("No thanks", (d, w) -> {
                Appoxee.instance().enablePush(false).enqueue(r -> {});
                savePushConsent(false);
            })
            .setCancelable(false)
            .show();
    }

    // -------------------------------------------------------------------------
    // getDevice — fetch device registration payload
    // -------------------------------------------------------------------------
    private void loadDeviceInfo() {
        Appoxee.instance().getDevice().enqueue(result -> {
            if (result.isSuccess()) {
                DevicePayload device = result.getData();
                // use device.getDeviceId(), device.getAlias(), etc.
            } else {
                String error = result.getError();
                // log or show error
            }
        });
    }

    // -------------------------------------------------------------------------
    // Implement these with SharedPreferences or your app's storage layer
    // -------------------------------------------------------------------------
    private String getCurrentUserId() { return null; }
    private boolean hasUserDecidedPushConsent() { return false; }
    private void savePushConsent(boolean accepted) {}
}
```

Note: `enablePush(true/false)` controls the Mapp backend opt-in state. For Android 13+,
request `POST_NOTIFICATIONS` separately (e.g. just before showing this dialog).

## Java-Specific Best Practices

- Keep Appoxee calls in dedicated manager/service classes to avoid activity bloat.
- Avoid anonymous callback duplication; extract reusable result handlers.
- Use explicit executor policy for all `execute()` usage.
- Keep legacy code style consistent with existing app conventions; avoid partial Kotlin migration unless requested.
