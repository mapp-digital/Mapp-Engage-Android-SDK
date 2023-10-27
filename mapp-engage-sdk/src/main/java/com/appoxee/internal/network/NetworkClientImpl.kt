@file:Suppress("BlockingMethodInNonBlockingContext")

package com.appoxee.internal.network

import android.util.Log
import com.appoxee.internal.network.exceptions.ClientException
import com.appoxee.internal.network.exceptions.RedirectException
import com.appoxee.internal.network.exceptions.ServerException
import com.appoxee.internal.network.exceptions.UnknownNetworkException
import com.appoxee.internal.network.response.Response
import com.appoxee.internal.network.response.ResponseAdapter
import com.appoxee.internal.util.convertToString
import com.appoxee.shared.AppoxeeOptions
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

internal class NetworkClientImpl(
    private val appoxeeOptions: AppoxeeOptions,
    private var readTime: Int = 10_000,
    private var connectionTime: Int = 10_000,
) : NetworkClient {

    private val TAG = NetworkClientImpl::class.java.name

    override suspend fun <T> execute(
        request: Request,
        responseAdapter: ResponseAdapter
    ): Response<T> {
        var json: JSONObject? = null
        val urlPath = buildUrl(request)
        val url = URL(urlPath)

        var response: Response<T> = responseAdapter.createResponse(0, null, null)

        (url.openConnection() as HttpURLConnection).run {
            try {
                readTimeout = readTime
                connectTimeout = connectionTime
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

                Log.w(
                    TAG,
                    "REQUEST - ${request.method.name.uppercase()}: $urlPath\nRequestBody: $data \nHeaders: ${
                        request.headers.map { "\"${it.key}\":\"${it.value}\"" }
                            .joinToString(separator = "\n")
                    }"
                )
                // retrieve request result
                val statusCode = responseCode
                val result = inputStream.convertToString()
                val error = errorStream.convertToString()
                val responseUrl = this.url

                Log.i(TAG, "\nRESPONSE - ${requestMethod}: ${responseUrl}\nResponseBody: $result")

                when (statusCode) {
                    in 200..299 -> {
                        // success
                        json = JSONObject(result)
                        response = responseAdapter.createResponse(statusCode, json, null)
                    }

                    in 300..399 -> {
                        // redirect
                        val throwable = RedirectException(
                            code = statusCode,
                            message = "Redirect exception",
                            cause = Throwable(error)
                        )
                        response = responseAdapter.createResponse(statusCode, null, throwable)
                    }

                    in 400..499 -> {
                        // request error
                        val throwable = ClientException(
                            code = statusCode,
                            message = "Client network request error",
                            cause = Throwable(error)
                        )
                        response = responseAdapter.createResponse(statusCode, null, throwable)
                    }

                    in 500..599 -> {
                        // server error
                        val throwable = ServerException(
                            code = statusCode,
                            message = "Server network error",
                            cause = Throwable(error)
                        )
                        response = responseAdapter.createResponse(statusCode, null, throwable)
                    }

                    else -> {
                        // unknown error
                        val throwable = UnknownNetworkException(
                            message = "Unknown network error",
                            cause = Throwable(error)
                        )
                        response = responseAdapter.createResponse(statusCode, null, throwable)
                    }
                }
            } catch (e: Exception) {
                response = responseAdapter.createResponse(-1, null, e)
            } finally {
                this.disconnect()
            }
        }
        return response
    }

    private fun buildUrl(request: Request): String {
        val queryPath = buildQueryPath(request)
        return StringBuilder()
            .append(appoxeeOptions.server.value)
            .append("/")
            .append(request.path)
            .append(queryPath)
            .toString()
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