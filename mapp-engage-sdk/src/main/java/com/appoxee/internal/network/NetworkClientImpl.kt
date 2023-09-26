@file:Suppress("BlockingMethodInNonBlockingContext")

package com.appoxee.internal.network

import com.appoxee.AppoxeeOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class NetworkClientImpl(
    private val appoxeeOptions: AppoxeeOptions,
) : NetworkClient {

    override suspend fun execute(request: Request): JSONObject? {
        var json: JSONObject? = null
        val urlPath = buildUrl(request)
        println("URL PATH: $urlPath")
        val url = URL(urlPath)

        (url.openConnection() as HttpURLConnection).run {
            readTimeout = 10_000
            connectTimeout = 10_000
            requestMethod = request.method.toString()
            doInput = true
            doOutput = true

            request.headers.entries.forEach {
                setRequestProperty(it.key, it.value)
            }

            DataOutputStream(outputStream).run {
                if (request.requestBody != null) {
                    val data = request.requestBody.asJson().toString()
                    write(data.toByteArray(Charsets.UTF_8))
                    flush()
                    close()
                }
            }

            val statusCode = responseCode
            val result = responseMessage

            print(result)

            when (statusCode) {
                in 200..299 -> {
                    // success
                    BufferedReader(InputStreamReader(inputStream)).run {
                        val sb = StringBuilder()
                        var line: String?
                        try {
                            do {
                                line = readLine()
                                sb.append(line)
                            } while (line != null)
                        } finally {
                            json = JSONObject(sb.toString())
                        }
                    }
                }

                in 300..399 -> {
                    // redirect
                }

                in 400..499 -> {
                    // request error
                }

                in 500..599 -> {
                    // server error
                }

                else -> {
                    // unknown error
                }
            }
        }
        return json
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