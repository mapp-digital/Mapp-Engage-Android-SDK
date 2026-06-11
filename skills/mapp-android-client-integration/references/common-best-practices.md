# Common Best Practices

Language-agnostic rules and validation reference. Load this alongside the
Kotlin or Java integration reference.

---

## Credentials — do NOT hardcode

`sdkKey`, `appId`, and `tenantId` must never appear as string literals in source.

**`local.properties`** (git-ignored — each developer fills their own):
```properties
mapp.sdk.key=YOUR_SDK_KEY
mapp.app.id=YOUR_APP_ID
mapp.tenant.id=YOUR_TENANT_ID
```

Expose via `BuildConfig` (see Kotlin/Java reference for Gradle snippet).
`server` is **not** a secret — keep it hardcoded in source.

---

## Correct dependency coordinates

```
group:    com.mapp.sdk
artifact: engage-android
version:  7.0.2   (latest published on Maven Central)

Firebase: com.google.firebase:firebase-messaging   (NOT firebase-messaging-ktx)
```

---

## Java / Kotlin version alignment

`compileOptions` (Java) and `kotlin { compilerOptions }` (Kotlin) must use the
same version number. Mismatch causes a build warning that escalates to an error
in newer AGP versions.

```kotlin
// inside android { }
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// top-level — new preferred form
// import org.jetbrains.kotlin.gradle.dsl.JvmTarget
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17   // matches VERSION_17 above
    }
}
```

Do **not** use the legacy `kotlinOptions { jvmTarget = "17" }` block inside
`android { }` — it is superseded by the `kotlin { compilerOptions }` block.
If both are present, remove `kotlinOptions`.

---

## Required files checklist

- [ ] `app/google-services.json` — from Firebase Console, placed in `app/` directory
- [ ] `local.properties` — `mapp.sdk.key`, `mapp.app.id`, `mapp.tenant.id` populated
- [ ] `local.properties` listed in `.gitignore`
- [ ] Application subclass declared in `AndroidManifest.xml` via `android:name`

---

## Integration sequence

1. Add SDK + Firebase dependencies; set `buildConfigField` in Gradle.
2. Add `google-services.json` to `app/`.
3. Add `mapp.*` keys to `local.properties`.
4. Register `Application` subclass in manifest.
5. Call `Appoxee.engage()` in `Application.onCreate()` on the main thread.
6. Subscribe `AppoxeeObserver` in entry Activity (`onStart`/`onStop`).
7. Inside `onReadyStatusChanged` (success only):
   a. `setAlias` — identify the user.
   b. Show push consent dialog if not yet decided → `enablePush(true/false)`.
   c. `triggerInApp("app_open")` — fire first in-app event.
8. Register push broadcast receiver via `setPushBroadcast()` after `engage()`.
9. Request `POST_NOTIFICATIONS` runtime permission (Android 13+).

---

## SDK usage guardrails

- Call `Appoxee.engage()` from the **main thread** in `Application.onCreate()` only.
- Never call `Appoxee.instance()` before `engage()` completes — NPE at runtime.
- Each `Call<T>` is **single-use**: call exactly one of `asSuspend()`, `enqueue()`,
  or `execute()`. A second call throws `CallConsumedException`.
- `execute()` is blocking — only use on a background/worker thread.
- `enablePush(true/false)` controls Mapp backend opt-in. It is **separate** from the
  Android `POST_NOTIFICATIONS` runtime permission — both are needed.
- Always handle `MappResult.Error` — never silently ignore failures.

---

## Correct import packages

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

Common wrong packages that will not compile:
- ~~`com.appoxee.AppoxeeObserver`~~ → `com.appoxee.shared.AppoxeeObserver`
- ~~`com.appoxee.device.DevicePayload`~~ → `com.appoxee.internal.model.response.DevicePayload`

---

## Common mistakes

- Initialising SDK in an `Activity` instead of `Application`.
- Calling `setAlias` / `enablePush` / `triggerInApp` before `onReadyStatusChanged` fires.
- Hardcoding `sdkKey`, `appId`, or `tenantId` in source files.
- Using `firebase-messaging-ktx` (does not exist) instead of `firebase-messaging`.
- Forgetting `POST_NOTIFICATIONS` runtime permission on Android 13+.
- Reusing the same `Call<T>` instance more than once.
- Swallowing `MappResult.Error` without logging or reporting.
