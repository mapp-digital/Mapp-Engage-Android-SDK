---
name: mapp-android-client-integration
description: Provide implementation help and best practices for integrating the Mapp Engage Android SDK into client Android apps. Use for setup, migration, troubleshooting, or feature implementation (push, identity, tags, inbox, in-app, geofencing). Detect whether the client app is Kotlin or legacy Java and provide language-specific implementation steps and code.
---

# Mapp Android Client Integration

Use this skill to produce concrete, implementation-ready guidance for client apps integrating this SDK.

## Workflow

1. Identify target language and module style.
- Check `build.gradle.kts`/`build.gradle` plugins and source sets:
  - Kotlin indicators: `id("org.jetbrains.kotlin.android")`, `.kt` codebase.
  - Java legacy indicators: no Kotlin plugin, `.java` codebase, callback-heavy code.
- If mixed project: default to the dominant style in that module. If user explicitly asks for Java, use Java.

2. Load only the needed references.
- Shared integration order and guardrails: `references/common-best-practices.md`
- Kotlin implementation path: `references/kotlin-integration.md`
- Java implementation path: `references/java-integration.md`

3. Return implementation guidance as file-by-file actions.
- Show exact files to touch (`Application`, manifest, Gradle files, optional FCM service).
- Include concise snippets aligned with the selected language.
- Call out placeholders that must be provided by the client: `sdkKey`, `appId`, `tenantId`, and `server`.

4. Enforce SDK usage guardrails.
- Initialize via `Appoxee.engage(application, options)` in `Application.onCreate()` on the main thread.
- Do not call `Appoxee.instance()` before successful `engage`.
- Treat each `Call<T>` as single-use (`asSuspend()` or `enqueue()` or `execute()`, not multiple on the same call).
- For Android 13+ (API 33+), include runtime permission handling for `POST_NOTIFICATIONS`.
- If using custom `FirebaseMessagingService`, route Mapp messages through `isPushMessageFromMapp` and `handlePushMessage`.

5. Close with verification steps.
- Build: run project-appropriate Gradle assemble task.
- Runtime: verify ready status, token registration, and push opt-in path.
- Feature checks as relevant: inbox fetch, in-app trigger, geofencing start/stop.

## Output Contract

When answering a client implementation request, structure the response in this order:

1. Language decision (`Kotlin` or `Java`) and why.
2. Required edits by file.
3. Minimal runnable snippet set.
4. Best-practice checks and common pitfalls to avoid.
