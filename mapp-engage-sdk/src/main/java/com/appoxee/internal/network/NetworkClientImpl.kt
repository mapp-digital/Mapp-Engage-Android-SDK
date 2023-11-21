@file:Suppress("BlockingMethodInNonBlockingContext")

package com.appoxee.internal.network

import com.appoxee.internal.network.exceptions.ClientException
import com.appoxee.internal.network.exceptions.RedirectException
import com.appoxee.internal.network.exceptions.ServerException
import com.appoxee.internal.network.exceptions.UnknownNetworkException
import com.appoxee.internal.network.response.Response
import com.appoxee.internal.network.response.ResponseAdapter
import com.appoxee.internal.util.Logger
import com.appoxee.internal.util.convertToString
import com.appoxee.internal.util.parseAsJSON
import com.appoxee.shared.AppoxeeOptions
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

internal class NetworkClientImpl(
    private val appoxeeOptions: AppoxeeOptions,
) : NetworkClient {

    private val TAG = NetworkClientImpl::class.java.name

    override suspend fun <T> execute(
        request: Request,
        adapter: ResponseAdapter<T>
    ): Response<T> {
        val urlPath = buildUrl(request)
        val url = URL(urlPath)

        var response: Response<T>

        (url.openConnection() as HttpURLConnection).run {
            try {
                readTimeout = appoxeeOptions.readTimeout
                connectTimeout = appoxeeOptions.connectionTimeout
                requestMethod = request.method.toString()
                doInput = request.doInput
                doOutput = request.doOutput

                request.headers.entries.forEach {
                    setRequestProperty(it.key, it.value)
                }

                // write request body if exists
                val data = request.requestBody?.asJson().toString()
                if (data.isNotEmpty()) {
                    DataOutputStream(outputStream).run {
                        write(data.toByteArray(Charsets.UTF_8))
                        flush()
                        close()
                    }
                }

                Logger.w(
                    TAG,
                    "REQUEST - ${request.method.name.uppercase()}: $urlPath\nRequestBody: $data \nHeaders: ${
                        request.headers.map { "\"${it.key}\":\"${it.value}\"" }
                            .joinToString(separator = "\n")
                    }"
                )
                // retrieve request result
                val statusCode = responseCode

                val result: String? = inputStream.convertToString()?.let {
                    Logger.i(
                        TAG,
                        "\nRESPONSE - ${requestMethod}: ${this.url}\nResponseBody: $it"
                    )
                    it
                }
                val error: String? = errorStream.convertToString()?.also {
                    Logger.e(
                        TAG,
                        "\nRESPONSE - ${requestMethod}: ${this.url}\nErrorBody: $it"
                    )
                }

                response = resolveResponse(adapter, statusCode, result, error)
            } catch (e: Exception) {
                val error: String? = errorStream.convertToString()?.also {
                    Logger.e(
                        TAG,
                        "\nRESPONSE - ${requestMethod}: ${this.url}\nErrorBody: $it"
                    )
                }
                response = resolveResponse(adapter, this.responseCode, null, error)
            } finally {
                this.disconnect()
            }
        }
        return response
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
                response = adapter.createResponse(statusCode, json, null)
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

    private fun buildUrl(request: Request): String {
        val queryPath = buildQueryPath(request)
        val sb = StringBuilder()

        if (request.pathType == Request.PathType.CEP) {
            sb.append(appoxeeOptions.server.internalCepUrl)
        } else {
            sb.append(appoxeeOptions.server.value)
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
            queryPath.append("${entry.key}=${entry.value}")
            if (index < request.queryParams.entries.size - 1) {
                queryPath.append("&")
            }
        }
        return queryPath.toString()
    }
}