package com.appoxee.internal.migration.data

internal data class OldRegistration(
    val alias: String? = null,
    val isRegistered: Boolean = false,
    val pushEnabled: Boolean = false,
    val pushToken: String? = null,
    val timestamp: Long = 0,
    val tags: Set<String> = emptySet(),
    val customAttributes: Map<String, Any?> = emptyMap(),
)
