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
import okhttp3.internal.platform.android.AndroidLogHandler.flush
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

        var response: Response<T>

        val connection: HttpURLConnection = provideHttpUrlConnection(url)

        connection.run {
            try {
                val options = getOptions()
                readTimeout = options.readTimeout
                connectTimeout = options.connectionTimeout
                requestMethod = request.method.toString()
                doInput = request.doInput
                doOutput = request.doOutput

                request.headers.entries.forEach {
                    setRequestProperty(it.key, it.value)
                }

                // write request body if exists
                val data = request.requestBody?.asJson().toString()
                Logger.w(
                    TAG,
                    "REQUEST - ${request.method.name.uppercase()}: $urlPath\nRequestBody: $data"
                )
                if (data.isNotEmpty()) {
                    DataOutputStream(outputStream).use { stream ->
                        stream.write(data.toByteArray(Charsets.UTF_8))
                        stream.flush()
                    }
                }
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
                val error: String = e.message+ errorStream.convertToString()?.also {
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
