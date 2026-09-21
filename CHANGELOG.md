# Changelog

All notable changes to the Mapp Engage Android SDK are documented in this file.

## [7.1.3] - [UNRELEASED]

### Bug Fixes

- **Backend-requested retries** — Failed requests now retry when response metadata contains `shouldRetry = true`, including HTTP errors and error metadata in HTTP 200 responses. Retries are limited to three, with delays of 1, 2, and 4 seconds.
- **Empty custom attribute resends** — `setAlias()` with `resendCustomAttributes = true` now skips resending custom attributes when the cache is empty, avoiding an unnecessary request with an empty `set` object.
- **Alias updates** — Successful `setAlias()` calls now save the requested alias and returned DMC user ID directly, without an immediate device GET. This prevents a temporary backend read delay from leaving the previous alias cached. Alias updates require successful response metadata and a non-empty DMC user ID before changing the cache.
- **Registration data persistence** — The alias and DMC user ID returned by registration are saved immediately, preserving other cached device fields. A failed follow-up device GET no longer clears the saved identity.
- **Device lookup alias** — Device GET requests now include the cached alias when available, including the alias saved from registration.
- **SDK logging configuration** — Exposed `AppoxeeOptions.logType` and made the logger honor it: `DEBUG` (the new default) logs only in debuggable host apps, while `RELEASE` also enables release logging. Supplied options override and save the logging setting; initialization without options restores the saved value. Changing only `logType` does not clear device registration.
- **Network request dispatching** — Public asynchronous SDK calls now execute blocking network operations on the I/O dispatcher instead of the default dispatcher, preventing network calls from occupying threads intended for CPU-bound work.
- **Network request logging** — Request details are now logged before opening the connection output stream, so logs accurately show when a request starts instead of appearing only after connection setup and request-body transmission.
- **Request payload serialization** — `SetAttributes`, tag updates, and in-app tracking payloads can now be converted to strings before their JSON representation has been requested, preventing uninitialized-property failures.
- **Geofence event timestamps** — Region status payloads now send the supplied event timestamp in the `timeStamp` field instead of incorrectly sending the device time-zone value.

### Improvements

- **Device refresh on SDK startup** — SDK initialization refreshes cached device data when the last successful device fetch is at least one hour old. The timestamp persists across app restarts, failed refreshes retain cached data, and explicit `getDevice()` calls still fetch from the backend.
- **Fewer registration requests** — Removed the duplicate device GET after push-token updates during registration. Registration retains one final device refresh; standalone push opt-in and opt-out updates retain their existing refresh behavior.

## [7.1.2] - 2026-07-21

### Bug Fixes

- **In-app web template display** — Fixed sizing and rendering issues that could prevent web-based in-app messages from being shown.

### Improvements

- **Mapp Intelligence integration** — The SDK now broadcasts the registered DMC user ID to installed Mapp Intelligence receivers after successful registration validation.

### Build

- **Version bump to 7.1.2** — Updated the published SDK version.

## [7.1.1] - 2026-07-07

### Bug Fixes

- **R8/ProGuard root-package collision fix** — Added explicit `-repackageclasses` for obfuscated internal classes and preserved public API entry points with focused `-keep` rules to prevent duplicate-root-package collisions when another obfuscated AAR is present.

### Build

- **Version bump to 7.1.1** — Updated the published SDK version.

## [7.1.0] - 2026-06-12

### Bug Fixes

- **`triggerInApp` missed messages on first launch** — On the first run after device registration, `triggerInApp` now retries fetching in-app messages up to 3 times with a 2-second delay between each attempt, stopping early as soon as a non-empty response is received. This replaces the previous single-shot approach that used a fixed 6-second upfront wait and frequently returned no messages because the backend had not finished processing the new registration.
- Updated 3rd party dependencies as regular maintainence to keep all up-to-date with applied fixes and security patches.

### Breaking Changes

- **Inbox public API types moved** — `InboxMessage`, `MessageStatus`, and `InboxMessagesResponse` have moved from `com.appoxee.internal.model.response.inbox` to `com.appoxee.shared`. Update your imports accordingly.
- TargetSdk 37 required.

### Build

- **Version bump to 7.1.0** — Updated the published SDK version.


## [7.0.2] - 2026-04-14

### Bug Fixes

- **Push opt-state token fallback** — `enablePush(Boolean, String?)` now trims the provided Firebase token and falls back to fetching a fresh token when the supplied value is blank, preventing opt-in / opt-out updates from failing due to empty token strings.
- **Notification mode update persistence** — `notificationMode` is now persisted correctly during SDK initialisation, so apps can change the mode after first launch instead of being stuck with the original value.
- **`SILENT_ONLY` notification mode restoration** — Restored the missing `NotificationMode.SILENT_ONLY` value and aligned push handling so silent-only mode suppresses notification UI while still processing push events.

### Improvements

- **Dependency alignment** — Updated project dependency recommendations and version catalog entries to current stable versions, including Kotlin 2.3.20, Firebase BOM 34.11.0, AndroidX Lifecycle 2.10.0, DataStore 1.2.1, Media3 1.10.0, and related test libraries.
- **Documentation refresh** — README requirements and integration snippets now document the current Kotlin, Gradle, AGP, and Firebase BOM versions, making the setup guidance consistent with the SDK build configuration.
- **Foreground detection cleanup** — Activity foreground tracking now uses lifecycle start/stop events, which makes notification display decisions more predictable.

### Build

- **Version bump to 7.0.2** — Updated the published SDK version.


## [7.0.1] - 2026-03-19

### Bug Fixes

- **Potential deadlock in storage layer** — Removed a redundant mutex around DataStore writes that could cause deadlocks when updating tags or custom attributes concurrently.
- **URL encoding for query parameters** — Query string keys and values are now properly percent-encoded, preventing malformed requests when parameters contain special characters.
- **Broadcast receiver reliability** — Fixed edge cases in push event handling that could cause missed or duplicated push notifications.
- **Proguard / consumer rules** — Corrected keep-rules to ensure SDK classes are not stripped in release builds.
- **v6 → v7 migration data loss on network failure** — v6 registration data (SharedPreferences and device file) is now only deleted after a confirmed successful device registration response. Previously, a network failure during the first v7 launch would silently discard all v6 data, causing the device to re-register as new on the next launch and lose its alias, tags, and push opt-in state.

### Improvements

- **Thread-safe SDK initialisation** — `Appoxee.engage()` now uses a double-checked lock for safe instance creation across threads, eliminating race conditions during app startup.
- **Reduced startup overhead** — Internal containers are now initialised lazily, improving app startup performance.

### Build

- **Gradle 9.3.0 / AGP 9.1.0** — SDK is now built with Gradle 9.3.0 and Android Gradle Plugin 9.1.0. Ensure your project is compatible if consuming sources directly.
