package com.zhihuminus.core.environment

import com.zhihuminus.data.executeZhihuAuthenticatedRequest
import com.zhihuminus.data.fetchZhihuAuthenticatedJson
import com.zhihuminus.util.Log
import com.zhihuminus.util.ZhihuCredentialRefresher
import com.zhihuminus.util.signZhihuFetchRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.ktor.http.ContentType as KtorContentType

interface ZhihuApiEnvironment {
    fun httpClient(): HttpClient

    fun mobileHttpClient(): HttpClient = httpClient()

    fun authenticatedCookies(): Map<String, String>

    suspend fun <T> withAuthenticatedClient(
        block: suspend (client: HttpClient, cookies: Map<String, String>) -> T,
    ): T = block(httpClient(), authenticatedCookies())

    suspend fun fetchJson(
        url: String,
        include: String,
    ): JsonObject? = withAuthenticatedClient { client, cookies ->
        fetchZhihuAuthenticatedJson(client, url) {
            method = HttpMethod.Get
            url {
                protocol = URLProtocol.HTTPS
                if (include.isNotEmpty()) {
                    parameters["include"] = include
                }
            }
            signZhihuFetchRequest(cookies)
        }
    }

    suspend fun signedGetText(url: String): String = withAuthenticatedClient { client, cookies ->
        executeZhihuAuthenticatedRequest(client, url) {
            method = HttpMethod.Get
            signZhihuFetchRequest(cookies)
        }.bodyAsText()
    }

    suspend fun refreshToken() {
        val client = httpClient()
        ZhihuCredentialRefresher.refreshZhihuToken(
            ZhihuCredentialRefresher.fetchRefreshToken(client),
            client,
        )
    }

    suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    )

    fun xsrfToken(): String = ""

    fun logDecodeFailure(
        tag: String?,
        item: JsonElement,
        error: Exception,
    ) {
        Log.e(tag ?: "ZhihuApiEnvironment", "Failed to decode item: $item", error)
    }
}

suspend fun ZhihuApiEnvironment.addReadHistory(
    contentToken: String,
    contentTypeName: String,
) {
    if (authenticatedCookies()["d_c0"] == null) return
    runCatching {
        postSigned("https://www.zhihu.com/api/v4/read_history/add") {
            contentType(KtorContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("content_token", contentToken)
                    put("content_type", contentTypeName)
                }.toString(),
            )
        }
    }
}

suspend fun ZhihuApiEnvironment.postSigned(
    url: String,
    block: HttpRequestBuilder.() -> Unit = {},
): HttpResponse = withAuthenticatedClient { client, cookies ->
    client.post(url) {
        block()
        signZhihuFetchRequest(cookies)
    }
}

suspend fun ZhihuApiEnvironment.deleteSigned(
    url: String,
    block: HttpRequestBuilder.() -> Unit = {},
): HttpResponse = withAuthenticatedClient { client, cookies ->
    client.delete(url) {
        block()
        signZhihuFetchRequest(cookies)
    }
}
