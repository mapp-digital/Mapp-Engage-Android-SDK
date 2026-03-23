# Changelog

All notable changes to the Mapp Engage Android SDK are documented in this file.

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
