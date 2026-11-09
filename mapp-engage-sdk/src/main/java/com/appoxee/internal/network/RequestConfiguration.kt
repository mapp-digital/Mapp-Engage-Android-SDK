package com.appoxee.internal.network

import com.appoxee.shared.AppoxeeOptions

/** Immutable channel settings shared by a request's body, headers, URL and retries. */
internal class RequestConfiguration(options: AppoxeeOptions) {
    val baseUrl: String = options.server.value
    val cepUrl: String = options.server.internalCepUrl
    val sdkKey: String = options.sdkKey
    val appId: String = options.appId
    val tenantId: String = options.tenantId
    val connectionTimeout: Int = options.connectionTimeout
    val readTimeout: Int = options.readTimeout

    fun headers(pathType: Request.PathType): Map<String, String> =
        if (pathType == Request.PathType.CEP) {
            mapOf("tenant_id" to tenantId, "app_id" to appId)
        } else {
            mapOf("X_KEY" to sdkKey)
        }
}
