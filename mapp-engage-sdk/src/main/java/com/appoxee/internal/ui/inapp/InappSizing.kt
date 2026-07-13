package com.appoxee.internal.ui.inapp

internal const val DEFAULT_INAPP_SIZE_PERCENT = 100

internal fun Int?.inappSizePercentOrDefault(): Int {
    return this?.takeIf { it > 0 } ?: DEFAULT_INAPP_SIZE_PERCENT
}
