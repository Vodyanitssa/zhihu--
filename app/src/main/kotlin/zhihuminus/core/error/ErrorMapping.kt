package com.zhihuminus.core.error

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException

private val errorJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/** 将异常转换为领域错误模型；文案层与日志层的唯一映射入口。 */
fun Throwable?.toAppError(): AppError {
    if (this == null) return AppError.Unknown("未知错误")
    return formatThrowable(unwrapException(this))
}

/** 兼容旧调用点：只取面向用户的文案。 */
fun friendlyErrorMessage(error: Throwable?): String = error.toAppError().userMessage

/** 构造带「action失败: 」前缀的错误；action 为中文动作名（如「加载用户信息」）。 */
fun failureMessage(
    action: String,
    error: Throwable?,
): AppError = error.toAppError().withPrefix(action)

private fun isTargetException(error: Throwable): Boolean =
    error is SocketTimeoutException ||
        error is java.net.SocketTimeoutException ||
        error is HttpRequestTimeoutException ||
        error is HttpStatusException ||
        error is UnknownHostException ||
        error is ConnectException ||
        error is SerializationException

private fun unwrapException(error: Throwable): Throwable {
    var current: Throwable = error
    while (current.cause != null && current.cause !== current) {
        if (isTargetException(current)) return current
        val cause = current.cause!!
        if (isTargetException(cause)) return cause
        current = cause
    }
    return current
}

private fun extractServerErrorMessage(bodyText: String): String? = runCatching {
    val root = errorJson.parseToJsonElement(bodyText).jsonObject
    val errorMessage = (root["error"] as? JsonObject)?.get("message")
        ?: root["message"]
        ?: root["error"]
    errorMessage?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
}.getOrNull()

private fun formatThrowable(error: Throwable): AppError = when (error) {
    is SocketTimeoutException,
    is java.net.SocketTimeoutException,
    is HttpRequestTimeoutException,
    -> AppError.Timeout("网络连接超时，请检查网络后重试", error)

    is UnknownHostException,
    is java.net.UnknownHostException,
    is ConnectException,
    is java.net.ConnectException,
    is ConnectTimeoutException,
    -> AppError.Network("无法连接到服务器，请检查网络设置", error)

    is HttpStatusException -> formatHttpStatus(error)

    is SerializationException -> AppError.Parse("数据解析失败，可能知乎接口已变更", error)

    is IOException -> formatIoException(error)

    is IllegalStateException -> AppError.Unknown(
        error.message?.takeIf { it.isNotBlank() && !it.contains("Exception") } ?: "操作状态异常",
        error,
    )

    else -> formatUnknown(error)
}

private fun formatHttpStatus(error: HttpStatusException): AppError {
    val serverMessage = extractServerErrorMessage(error.bodyText)
    if (serverMessage != null) {
        return AppError.Business(serverMessage, cause = error)
    }
    val status = error.status.value
    return when (status) {
        401 -> AppError.Auth("登录已过期，请重新登录", error)
        403 -> AppError.Http(status, "没有访问权限或内容已被封禁", error)
        404 -> AppError.Http(status, "内容不存在或已被删除", error)
        429 -> AppError.Http(status, "请求过于频繁，请稍后再试", error)
        in 500..599 -> AppError.Http(status, "知乎服务器开小差了，请稍后重试 ($status)", error)
        else -> AppError.Http(status, "网络请求失败 ($status)", error)
    }
}

private fun formatIoException(error: IOException): AppError {
    val message = error.message?.lowercase().orEmpty()
    return when {
        "timeout" in message || "timed out" in message ->
            AppError.Timeout("网络连接超时，请检查网络后重试", error)

        "connection refused" in message || "failed to connect" in message ->
            AppError.Network("无法连接到服务器，请检查网络设置", error)

        "reset" in message || "broken pipe" in message ->
            AppError.Network("网络连接异常中断，请重试", error)

        "no route to host" in message ->
            AppError.Network("无法访问网络，请检查网络设置", error)

        else -> AppError.Network(
            error.message?.takeIf { it.isNotBlank() && !it.contains("Exception") } ?: "网络通信异常，请重试",
            error,
        )
    }
}

private fun formatUnknown(error: Throwable): AppError {
    val message = error.message?.lowercase().orEmpty()
    return when {
        "timeout" in message || "timed out" in message ->
            AppError.Timeout("网络连接超时，请检查网络后重试", error)

        "unknown host" in message || "unable to resolve host" in message ->
            AppError.Network("无法解析服务器地址，请检查网络设置", error)

        "no route to host" in message ->
            AppError.Network("无法连接到网络，请检查网络设置", error)

        !error.message.isNullOrBlank() && !error.message!!.contains("Exception") ->
            AppError.Unknown(error.message!!, error)

        else -> AppError.Unknown("发生未知错误，请重试", error)
    }
}
