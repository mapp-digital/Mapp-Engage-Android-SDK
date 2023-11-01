package com.appoxee.internal.network

internal abstract class Request(
    val path: String,
    val method: Method,
    val queryParams: Map<String, Any>? = null,
    val requestBody: NetworkData? = null,
    val headers: MutableMap<String, String> = mutableMapOf(
        "Content-Type" to "application/json; utf-8",
        "Accept" to "application/json"
    ),

    ) {
    internal var doInput: Boolean = true
    internal var doOutput: Boolean = true

    internal var pathType: PathType = PathType.BASE

    internal fun addHeader(header: Map<String, String>): Request {
        headers.putAll(header)
        return this
    }

    internal fun setPathType(pathType: PathType): Request {
        this.pathType = pathType
        return this
    }

    internal class Get(path: String, queryParams: Map<String, Any>? = null) :
        Request(path = path, method = Method.GET, queryParams = queryParams) {
    }

    internal class Put(path: String, requestBody: NetworkData? = null) :
        Request(path = path, method = Method.PUT, requestBody = requestBody) {
    }

    internal class Post(
        path: String,
        queryParams: Map<String, Any>? = emptyMap(),
        requestBody: NetworkData? = null
    ) : Request(
        path = path,
        method = Method.POST,
        queryParams = queryParams,
        requestBody = requestBody
    ) {

    }

    internal enum class Method(private val value: String) {
        GET("GET"),
        POST("POST"),
        PUT("PUT"),
        PATCH("PATCH"),
        DELETE("DELETE")
    }

    internal enum class PathType(private val value: String) {
        BASE("BASE"),
        CEP("CEP")
    }
}