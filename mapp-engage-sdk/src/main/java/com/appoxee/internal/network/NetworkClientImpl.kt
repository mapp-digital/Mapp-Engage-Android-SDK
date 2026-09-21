@file:Suppress("BlockingMethodInNonBlockingContext")

package com.appoxee.internal.network

import com.appoxee.internal.network.exceptions.ClientException
import com.appoxee.internal.network.exceptions.DeviceNotRegisteredException
import com.appoxee.internal.network.exceptions.RedirectException
import com.appoxee.internal.network.exceptions.ServerException
import com.appoxee.internal.network.exceptions.UnknownNetworkException
import com.appoxee.internal.network.response.Response
import com.appoxee.internal.network.response.ResponseAdapter
import com.appoxee.internal.storage.Storage
import com.appoxee.internal.util.Logger
import com.appoxee.internal.util.convertToString
import com.appoxee.internal.util.parseAsJSON
import com.appoxee.shared.AppoxeeOptions
import kotlinx.coroutines.delay
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

internal class NetworkClientImpl(
    private val storage: Storage
) : NetworkClient {

    private val TAG = NetworkClientImpl::class.java.name

    private lateinit var options: AppoxeeOptions
    private suspend fun getOptions(): AppoxeeOptions {
        if (!::options.isInitialized) {
            options = storage.getInitOptions() ?: throw DeviceNotRegisteredException()
        }
        return options
    }

    override suspend fun <T> execute(
        request: Request,
        adapter: ResponseAdapter<T>
    ): Response<T> {
        val urlPath = buildUrl(request)
        val url = provideUrl(urlPath)
        val options = getOptions()
        var retries = 0

        while (true) {
            val connection = provideHttpUrlConnection(url)
            val statusCode: Int
            val body: String?
            try {
                connection.run {
                    readTimeout = options.readTimeout
                    connectTimeout = options.connectionTimeout
                    requestMethod = request.method.toString()
                    doInput = request.doInput
                    doOutput = request.doOutput
                    request.headers.forEach { (key, value) -> setRequestProperty(key, value) }

                    val data = request.requestBody?.asJson()?.toString()
                    Logger.w(TAG, "REQUEST - ${request.method}: $urlPath\nRequestBody: $data")
                    if (!data.isNullOrEmpty()) {
                        DataOutputStream(outputStream).use { stream ->
                            stream.write(data.toByteArray(Charsets.UTF_8))
                            stream.flush()
                        }
                    }
                }
                statusCode = connection.responseCode
                body = (if (statusCode >= 400) connection.errorStream else connection.inputStream)
                    .convertToString()
                Logger.i(TAG, "RESPONSE - ${request.method}: $urlPath\nResponseBody: $body")
            } finally {
                connection.disconnect()
            }

            // Inspect metadata before adapters parse the (possibly empty) error payload.
            val metadata = body.parseAsJSON().optJSONObject("metadata")
            val failed = statusCode !in 200..299 || metadata?.optBoolean("error", false) == true
            if (failed && metadata?.optBoolean("shouldRetry", false) == true && retries < MAX_RETRIES) {
                val delayMs = INITIAL_RETRY_DELAY_MS * (1L shl retries)
                retries++
                Logger.d(TAG, "Backend requested retry $retries/$MAX_RETRIES in ${delayMs}ms")
                delay(delayMs)
                continue
            }
            return resolveResponse(adapter, statusCode, body, body)
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
        const val INITIAL_RETRY_DELAY_MS = 1000L
    }

    private fun provideHttpUrlConnection(url: URL): HttpURLConnection {
        return url.openConnection() as HttpURLConnection
    }

    private fun provideUrl(path: String): URL {
        return URL(path)
    }

    private fun <T> resolveResponse(
        adapter: ResponseAdapter<T>,
        statusCode: Int,
        result: String?,
        error: String?
    ): Response<T> {
        val response: Response<T>
        when (statusCode) {
            in 200..299 -> {
                // success
                val json = result.parseAsJSON()
                val metadata = json.optJSONObject("metadata")
                response = if (metadata?.optBoolean("error", false) == true) {
                    Response.error(Throwable(metadata.optString("errorMessage")))
                } else {
                    adapter.createResponse(statusCode, json, null)
                }
            }

            in 300..399 -> {
                // redirect
                throw RedirectException(
                    code = statusCode,
                    message = "Redirect exception",
                    cause = Throwable(error)
                )
            }

            in 400..499 -> {
                // request error
                throw ClientException(
                    code = statusCode,
                    message = "Client network request error",
                    cause = Throwable(error)
                )
            }

            in 500..599 -> {
                // server error
                throw ServerException(
                    code = statusCode,
                    message = "Server network error",
                    cause = Throwable(error)
                )
            }

            else -> {
                // unknown error
                throw UnknownNetworkException(
                    message = "Unknown network error",
                    cause = Throwable(error)
                )
            }
        }
        return response
    }

    private suspend fun buildUrl(request: Request): String {
        val queryPath = buildQueryPath(request)
        val sb = StringBuilder()
        val options = getOptions()

        if (request.pathType == Request.PathType.CEP) {
            sb.append(options.server.internalCepUrl)
        } else {
            sb.append(options.server.value)
        }

        sb.append("/")
            .append(request.path)
            .append(queryPath)

        return sb.toString()
    }

    private fun buildQueryPath(request: Request): String {
        val queryPath = StringBuilder()
        request.queryParams?.entries?.mapIndexed { index, entry ->
            if (index == 0) {
                queryPath.append("?")
            }
            queryPath.append("${URLEncoder.encode(entry.key, "UTF-8")}=${URLEncoder.encode(entry.value.toString(), "UTF-8")}")
            if (index < request.queryParams.entries.size - 1) {
                queryPath.append("&")
            }
        }
        return queryPath.toString()
    }
}
