# Changelog

All notable changes to the Mapp Engage Android SDK are documented in this file.

## [7.0.2] - UNRELEASED

### Bug Fixes

- **Push opt-state token fallback** — `enablePush(Boolean, String?)` now trims the provided Firebase token and falls back to fetching a fresh token when the supplied value is blank, preventing opt-in / opt-out updates from failing due to empty token strings.

### Improvements

- **Dependency alignment** — Updated project dependency recommendations and version catalog entries to current stable versions, including Kotlin 2.3.20, Firebase BOM 34.11.0, AndroidX Lifecycle 2.10.0, DataStore 1.2.1, Media3 1.10.0, and related test libraries.
- **Documentation refresh** — README requirements and integration snippets now document the current Kotlin, Gradle, AGP, and Firebase BOM versions, making the setup guidance consistent with the SDK build configuration.

### Build

- **Version bump to 7.0.2** — Updated the published SDK version and refreshed the sample consumer version reference to `7.0.1` for released dependency usage.

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
