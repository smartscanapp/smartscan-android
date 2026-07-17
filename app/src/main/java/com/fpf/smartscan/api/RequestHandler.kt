package com.fpf.smartscan.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ResponseData(
    val statusCode: Int,
    val body: String
)

data class HttpException(val statusCode: Int, override val message: String) : Exception("$statusCode: $message")

enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH
}

class RequestHandler(private val baseUrl: String? = null) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun makeRequest(url: String, method: HttpMethod, headers: Map<String, String>? = null, body: RequestBody? = null
    ): Result<ResponseData> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder().url(buildUrl(url))

                headers?.forEach { (key, value) ->
                    requestBuilder.addHeader(key, value)
                }

                when (method) {
                    HttpMethod.POST -> requestBuilder.post(body ?: defaultBody())
                    HttpMethod.PUT -> requestBuilder.put(body ?: defaultBody())
                    HttpMethod.DELETE -> {
                        if (body != null) {
                            requestBuilder.delete(body)
                        } else {
                            requestBuilder.delete()
                        }
                    }
                    HttpMethod.PATCH -> requestBuilder.patch(body ?: defaultBody())
                    HttpMethod.GET -> requestBuilder.get()
                }

                val request = requestBuilder.build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    ResponseData(
                        statusCode = response.code,
                        body = response.body.string()
                    ).let {
                        Result.success(it)
                    }
                } else {
                    Result.failure(HttpException(response.code, response.body.string()))
                }
            } catch (e: IOException) {
                Result.failure(e)
            }
        }
    }

    private fun buildUrl(endpoint: String): String {
        return if (baseUrl.isNullOrEmpty()) endpoint else "$baseUrl$endpoint"
    }

    private fun defaultBody(): RequestBody =
        "{}".toRequestBody("application/json".toMediaTypeOrNull())
}