package com.zhihuminus.core.util

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

/**
 * 友好业务领域异常封装。
 */
class FriendlyException(
    val friendlyMessage: String,
    cause: Throwable? = null,
) : RuntimeException(friendlyMessage, cause)

/**
 * 将异常转换为友好的本地化错误文本。
 */
val Throwable.friendlyMessage: String
    get() = friendlyErrorMessage(this)

/**
 * 将当前异常转换为领域友好的 [FriendlyException]。
 */
fun Throwable.toFriendlyException(): FriendlyException =
    if (this is FriendlyException) this else FriendlyException(friendlyErrorMessage(this), this)

/**
 * 安全执行操作，若捕获异常则转换为友好错误（透传协程取消异常 CancellationException）。
 */
inline fun <T> runCatchingFriendly(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e.toFriendlyException())
}

fun friendlyErrorMessage(error: Throwable?): String {
    if (error == null) return "未知错误"
    val root = unwrapException(error)
    return formatThrowable(root)
}

private fun unwrapException(error: Throwable): Throwable {
    var current: Throwable = error
    while (current.cause != null && current.cause !== current) {
        if (current is FriendlyException ||
            current is SocketTimeoutException ||
            current is java.net.SocketTimeoutException ||
            current is HttpRequestTimeoutException ||
            current is HttpStatusException ||
            current is UnknownHostException ||
            current is ConnectException ||
            current is SerializationException
        ) {
            return current
        }
        val cause = current.cause!!
        if (cause is FriendlyException ||
            cause is SocketTimeoutException ||
            cause is java.net.SocketTimeoutException ||
            cause is HttpRequestTimeoutException ||
            cause is HttpStatusException ||
            cause is UnknownHostException ||
            cause is ConnectException ||
            cause is SerializationException
        ) {
            return cause
        }
        current = cause
    }
    return current
}

private fun formatThrowable(error: Throwable): String = when (error) {
    is FriendlyException -> error.friendlyMessage

    is SocketTimeoutException,
    is java.net.SocketTimeoutException,
    is HttpRequestTimeoutException,
    -> "网络连接超时，请检查网络后重试"

    is UnknownHostException,
    is java.net.UnknownHostException,
    is ConnectException,
    is java.net.ConnectException,
    is ConnectTimeoutException,
    -> "无法连接到服务器，请检查网络设置"

    is HttpStatusException -> when (error.status.value) {
        401 -> "登录已过期，请重新登录"
        403 -> "没有访问权限或内容已被封禁"
        404 -> "内容不存在或已被删除"
        429 -> "请求过于频繁，请稍后再试"
        in 500..599 -> "知乎服务器开小差了，请稍后重试 (${error.status.value})"
        else -> "网络请求失败 (${error.status.value})"
    }

    is SerializationException -> "数据解析失败，可能知乎接口已变更"

    is IOException -> {
        val msg = error.message?.lowercase().orEmpty()
        when {
            "timeout" in msg || "timed out" in msg -> "网络连接超时，请检查网络后重试"
            "connection refused" in msg || "failed to connect" in msg -> "无法连接到服务器，请检查网络设置"
            "reset" in msg || "broken pipe" in msg -> "网络连接异常中断，请重试"
            "no route to host" in msg -> "无法访问网络，请检查网络设置"
            else -> error.message?.takeIf { it.isNotBlank() && !it.contains("Exception") }
                ?: "网络通信异常，请重试"
        }
    }

    is IllegalStateException -> error.message?.takeIf { it.isNotBlank() && !it.contains("Exception") }
        ?: "操作状态异常"

    else -> {
        val msg = error.message?.lowercase().orEmpty()
        when {
            "timeout" in msg || "timed out" in msg -> "网络连接超时，请检查网络后重试"
            "unknown host" in msg || "unable to resolve host" in msg -> "无法解析服务器地址，请检查网络设置"
            "no route to host" in msg -> "无法连接到网络，请检查网络设置"
            !error.message.isNullOrBlank() && !error.message!!.contains("Exception") -> error.message!!
            else -> "发生未知错误，请重试"
        }
    }
}
