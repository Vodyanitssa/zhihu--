package com.zhihuminus.core.util

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FriendlyErrorTest {
    @Test
    fun handlesNullError() {
        assertEquals("未知错误", friendlyErrorMessage(null))
    }

    @Test
    fun handlesTimeoutExceptions() {
        assertEquals(
            "网络连接超时，请检查网络后重试",
            friendlyErrorMessage(SocketTimeoutException("Socket timed out")),
        )
        assertEquals(
            "网络连接超时，请检查网络后重试",
            friendlyErrorMessage(java.net.SocketTimeoutException("connect timed out")),
        )
        assertEquals(
            "网络连接超时，请检查网络后重试",
            friendlyErrorMessage(HttpRequestTimeoutException("https://www.zhihu.com", 15000L)),
        )
    }

    @Test
    fun handlesConnectionExceptions() {
        assertEquals(
            "无法连接到服务器，请检查网络设置",
            friendlyErrorMessage(ConnectException("Connection refused")),
        )
        assertEquals(
            "无法连接到服务器，请检查网络设置",
            friendlyErrorMessage(UnknownHostException("api.zhihu.com")),
        )
        assertEquals(
            "无法连接到服务器，请检查网络设置",
            friendlyErrorMessage(ConnectTimeoutException("connect timeout")),
        )
    }

    @Test
    fun handlesHttpStatusExceptions() {
        val notFound = HttpStatusException(HttpStatusCode.NotFound, Url("https://www.zhihu.com"), "not found")
        val unauthorized = HttpStatusException(HttpStatusCode.Unauthorized, Url("https://www.zhihu.com"), "unauthorized")
        val serverError = HttpStatusException(HttpStatusCode.InternalServerError, Url("https://www.zhihu.com"), "server error")
        val gatewayTimeout = HttpStatusException(HttpStatusCode.GatewayTimeout, Url("https://www.zhihu.com"), "gateway timeout")

        assertEquals("内容不存在或已被删除", friendlyErrorMessage(notFound))
        assertEquals("登录已过期，请重新登录", friendlyErrorMessage(unauthorized))
        assertEquals("知乎服务器开小差了，请稍后重试 (500)", friendlyErrorMessage(serverError))
        assertEquals("知乎服务器开小差了，请稍后重试 (504)", friendlyErrorMessage(gatewayTimeout))
    }

    @Test
    fun handlesSerializationExceptions() {
        assertEquals(
            "数据解析失败，可能知乎接口已变更",
            friendlyErrorMessage(SerializationException("Missing field")),
        )
    }

    @Test
    fun unwrapsNestedExceptions() {
        val root = SocketTimeoutException("deep timeout")
        val wrapped = RuntimeException("Outer wrapper", IOException("Middle wrapper", root))
        assertEquals(
            "网络连接超时，请检查网络后重试",
            friendlyErrorMessage(wrapped),
        )
    }

    @Test
    fun handlesGenericIoExceptions() {
        val brokenPipe = IOException("write failed: Broken pipe")
        assertTrue(friendlyErrorMessage(brokenPipe).contains("中断"))
    }
}
