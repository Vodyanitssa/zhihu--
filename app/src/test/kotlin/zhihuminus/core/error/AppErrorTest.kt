package com.zhihuminus.core.error

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AppErrorTest {
    @Test
    fun handlesNullError() {
        assertEquals("未知错误", friendlyErrorMessage(null))
        assertIs<AppError.Unknown>(null.toAppError())
    }

    @Test
    fun handlesTimeoutExceptions() {
        val errors = listOf(
            SocketTimeoutException("Socket timed out"),
            java.net.SocketTimeoutException("connect timed out"),
            HttpRequestTimeoutException("https://www.zhihu.com", 15000L),
        )
        errors.forEach { error ->
            assertEquals("网络连接超时，请检查网络后重试", friendlyErrorMessage(error))
            val appError = assertIs<AppError.Timeout>(error.toAppError())
            assertTrue(appError.retryable)
            assertEquals(error, appError.cause)
        }
    }

    @Test
    fun handlesConnectionExceptions() {
        val errors = listOf(
            ConnectException("Connection refused"),
            UnknownHostException("api.zhihu.com"),
            ConnectTimeoutException("connect timeout"),
        )
        errors.forEach { error ->
            assertEquals("无法连接到服务器，请检查网络设置", friendlyErrorMessage(error))
            assertTrue(assertIs<AppError.Network>(error.toAppError()).retryable)
        }
    }

    @Test
    fun handlesHttpStatusExceptions() {
        val notFound = HttpStatusException(HttpStatusCode.NotFound, Url("https://www.zhihu.com"), "not found")
        val unauthorized = HttpStatusException(HttpStatusCode.Unauthorized, Url("https://www.zhihu.com"), "unauthorized")
        val forbidden = HttpStatusException(HttpStatusCode.Forbidden, Url("https://www.zhihu.com"), "forbidden")
        val tooManyRequests = HttpStatusException(HttpStatusCode.TooManyRequests, Url("https://www.zhihu.com"), "429")
        val serverError = HttpStatusException(HttpStatusCode.InternalServerError, Url("https://www.zhihu.com"), "server error")
        val gatewayTimeout = HttpStatusException(HttpStatusCode.GatewayTimeout, Url("https://www.zhihu.com"), "gateway timeout")

        assertEquals("内容不存在或已被删除", friendlyErrorMessage(notFound))
        assertEquals("登录已过期，请重新登录", friendlyErrorMessage(unauthorized))
        assertEquals("知乎服务器开小差了，请稍后重试 (500)", friendlyErrorMessage(serverError))
        assertEquals("知乎服务器开小差了，请稍后重试 (504)", friendlyErrorMessage(gatewayTimeout))

        assertIs<AppError.Auth>(unauthorized.toAppError())
        assertEquals(
            HttpStatusCode.NotFound.value,
            assertIs<AppError.Http>(notFound.toAppError()).status,
        )
        assertTrue(!forbidden.toAppError().retryable)
        assertTrue(tooManyRequests.toAppError().retryable)
        assertTrue(serverError.toAppError().retryable)
    }

    @Test
    fun handlesServerProvidedErrorMessage() {
        val body = """{"error":{"code":310000,"name":"UserRegulateError","message":"该账号已停用，主页无法访问"}}"""
        val gone = HttpStatusException(HttpStatusCode.Gone, Url("https://api.zhihu.com/people/89-14-88-24-6"), body)
        assertEquals("该账号已停用，主页无法访问", friendlyErrorMessage(gone))
        val business = assertIs<AppError.Business>(gone.toAppError())
        assertEquals("该账号已停用，主页无法访问", business.serverMessage)
        assertTrue(!business.retryable)

        val rootMessage = HttpStatusException(HttpStatusCode.BadRequest, Url("https://www.zhihu.com"), """{"message":"参数不合法"}""")
        assertEquals("参数不合法", friendlyErrorMessage(rootMessage))
    }

    @Test
    fun fallsBackWhenServerMessageIsMissing() {
        val html = HttpStatusException(HttpStatusCode.Gone, Url("https://www.zhihu.com"), "<html>gone</html>")
        assertEquals("网络请求失败 (410)", friendlyErrorMessage(html))

        val empty = HttpStatusException(HttpStatusCode.InternalServerError, Url("https://www.zhihu.com"), "{}")
        assertEquals("知乎服务器开小差了，请稍后重试 (500)", friendlyErrorMessage(empty))
    }

    @Test
    fun handlesSerializationExceptions() {
        val error = SerializationException("Missing field")
        assertEquals("数据解析失败，可能知乎接口已变更", friendlyErrorMessage(error))
        assertTrue(!error.toAppError().retryable)
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

    @Test
    fun withPrefixUsesHalfWidthColon() {
        val error = HttpStatusException(HttpStatusCode.Gone, Url("https://www.zhihu.com"), "gone")
        val prefixed = failureMessage("加载用户信息", error)
        assertEquals("加载用户信息失败: 网络请求失败 (410)", prefixed.userMessage)

        val serverBody = HttpStatusException(HttpStatusCode.Gone, Url("https://www.zhihu.com"), """{"error":{"message":"该账号已停用"}}""")
        assertEquals("加载用户信息失败: 该账号已停用", failureMessage("加载用户信息", serverBody).userMessage)
    }

    @Test
    fun reportFailureRethrowsCancellation() {
        assertFailsWith<CancellationException> {
            reportFailure("AppErrorTest", "load data", CancellationException("cancelled"))
        }
    }

    @Test
    fun reportFailureReturnsPrefixedError() {
        val error = UnknownHostException("no internet")
        val appError = reportFailure("AppErrorTest", "load data", error, userAction = "加载数据")
        assertEquals("加载数据失败: 无法连接到服务器，请检查网络设置", appError.userMessage)
        assertTrue(appError.retryable)
        assertEquals(error, appError.cause)
    }
}
