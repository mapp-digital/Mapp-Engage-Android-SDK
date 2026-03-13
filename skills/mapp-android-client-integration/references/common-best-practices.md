# Common Best Practices

Use this file for language-agnostic guidance before applying Kotlin- or Java-specific snippets.

## Integration Sequence

1. Add SDK + Firebase dependencies in the app module.
2. Add required permissions in `AndroidManifest.xml`.
3. Initialize SDK in `Application.onCreate()` with client channel values.
4. Decide push mode:
- Default mode: keep SDK-managed `MappMessagingService`.
- Custom mode: wire a custom `FirebaseMessagingService` and forward only Mapp payloads.
5. Request `POST_NOTIFICATIONS` at runtime on Android 13+.
6. Validate with one end-to-end push flow.

## Required Inputs From Client

- `server` (`AppoxeeOptions.Server.*`)
- `sdkKey`
- `appId`
- `tenantId`

Never invent real credentials. Use placeholders when drafting integration snippets.

## Guardrails

- Call `Appoxee.engage()` from the main thread in `Application.onCreate()`.
- Call `Appoxee.instance()` only after `engage()`.
- Consume each `Call<T>` once only.
- Use non-blocking APIs in UI paths:
- Kotlin: prefer `asSuspend()` in coroutine scope.
- Java: prefer `enqueue(...)`; use `execute()` only on worker thread.
- Always handle both success and error `MappResult`.

## Push and Messaging Notes

- Optional push event receiver can be registered via `setPushBroadcast(...)`.
- If custom FCM service is used, include:
- `isPushMessageFromMapp(remoteMessage)`
- `handlePushMessage(remoteMessage)`
- `updateFirebaseToken(token)` in `onNewToken`.

## Common Mistakes

- Initializing in an `Activity` instead of `Application`.
- Forgetting runtime notification permission on API 33+.
- Reusing the same `Call<T>` instance twice.
- Swallowing `MappResult.Error` without logging/reporting.
