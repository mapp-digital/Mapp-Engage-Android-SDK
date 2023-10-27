package com.appoxee.internal.network

internal abstract class Request(
    val path: String,
    val method: Method,
    val queryParams: Map<String, Any>? = null,
    val requestBody: NetworkData? = null,
    var headers: MutableMap<String, String> = mutableMapOf(
        "Content-Type" to "application/json; utf-8",
        "Accept" to "application/json"
    )
) {

    var doInput: Boolean = true
    var doOutput: Boolean = true

    fun addHeader(header: Map<String, String>): Request {
        headers.putAll(header)
        return this
    }

    class Get(path: String, queryParams: Map<String, Any>? = null) :
        Request(path = path, method = Method.GET, queryParams = queryParams) {
    }

    class Put(path: String, requestBody: NetworkData? = null) :
        Request(path = path, method = Method.PUT, requestBody = requestBody) {
    }

    class Post(path: String, queryParams: Map<String, Any>?, requestBody: NetworkData?) : Request(
        path = path,
        method = Method.POST,
        queryParams = queryParams,
        requestBody = requestBody
    ) {

    }

    enum class Method(private val value: String) {
        GET("GET"),
        POST("POST"),
        PUT("PUT"),
        PATCH("PATCH"),
        DELETE("DELETE")
    }
}