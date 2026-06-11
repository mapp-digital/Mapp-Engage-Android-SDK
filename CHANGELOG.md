# Changelog

All notable changes to the Mapp Engage Android SDK are documented in this file.

## [7.0.3] - 2026-06-11

### Bug Fixes

- **`triggerInApp` missed messages on first launch** — On the first run after device registration, `triggerInApp` now retries fetching in-app messages up to 3 times with a 2-second delay between each attempt, stopping early as soon as a non-empty response is received. This replaces the previous single-shot approach that used a fixed 6-second upfront wait and frequently returned no messages because the backend had not finished processing the new registration.

### Breaking Changes

- **Inbox public API types moved** — `InboxMessage`, `MessageStatus`, and `InboxMessagesResponse` have moved from `com.appoxee.internal.model.response.inbox` to `com.appoxee.shared`. Update your imports accordingly.

### Build

- **Version bump to 7.0.3** — Updated the published SDK version.


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
