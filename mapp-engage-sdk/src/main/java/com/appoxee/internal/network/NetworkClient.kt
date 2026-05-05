package com.appoxee.internal.network

import com.appoxee.internal.network.response.Response
import com.appoxee.internal.network.response.ResponseAdapter

internal interface NetworkClient {
    suspend fun <T> execute(request: Request, adapter: ResponseAdapter<T>): Response<T>
}