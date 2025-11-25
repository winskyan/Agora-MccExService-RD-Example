package io.agora.mccex_demo.net

import io.agora.mccex_demo.utils.LogUtils
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object NetworkClient {
    private val client = OkHttpClient()
    fun sendHttpsRequest(
        url: String,
        headers: Map<*, *>,
        body: String,
        method: Method,
        call: Callback
    ) {
        LogUtils.d("sendHttpsRequest: url = $url, headers = $headers, body = $body, method = $method")
        val requestBuilder = Request.Builder().url(url)

        // 添加请求头部
        for ((key, value) in headers) {
            requestBuilder.addHeader(key.toString(), value.toString())
        }


        val request: Request = when (method) {
            Method.GET -> {
                requestBuilder.get().build()
            }

            Method.POST -> {
                // 构建请求体
                val requestBody = body.toRequestBody("application/json".toMediaTypeOrNull())
                // 发送POST请求
                requestBuilder.post(requestBody).build()
            }

            Method.DELETE -> {
                requestBuilder.delete().build()
            }
        }

        client.newCall(request).enqueue(call)
    }

    enum class Method {
        GET, POST, DELETE
    }
}