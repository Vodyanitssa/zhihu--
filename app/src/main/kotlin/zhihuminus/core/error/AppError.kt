package com.zhihuminus.core.error

/**
 * 领域错误模型：统一承载面向用户的文案、能否重试以及原始异常。
 *
 * [userMessage] 只描述错误本身，不含业务动作前缀；需要「xx失败: 」前缀时使用 [withPrefix]。
 */
sealed interface AppError {
    val userMessage: String
    val cause: Throwable?
    val retryable: Boolean

    /** 网络不可达、DNS 解析失败、连接中断等。 */
    data class Network(
        override val userMessage: String,
        override val cause: Throwable? = null,
    ) : AppError {
        override val retryable: Boolean get() = true
    }

    /** 连接或请求超时。 */
    data class Timeout(
        override val userMessage: String,
        override val cause: Throwable? = null,
    ) : AppError {
        override val retryable: Boolean get() = true
    }

    /** 登录态失效，需要重新登录。 */
    data class Auth(
        override val userMessage: String,
        override val cause: Throwable? = null,
    ) : AppError {
        override val retryable: Boolean get() = false
    }

    /** 知乎服务端返回的业务错误，[serverMessage] 为原始中文提示。 */
    data class Business(
        val serverMessage: String,
        override val userMessage: String = serverMessage,
        override val cause: Throwable? = null,
    ) : AppError {
        override val retryable: Boolean get() = false
    }

    /** HTTP 状态码错误，[status] 为三位状态码。 */
    data class Http(
        val status: Int,
        override val userMessage: String,
        override val cause: Throwable? = null,
    ) : AppError {
        override val retryable: Boolean get() = status in 500..599 || status == 429
    }

    /** 响应解析失败。 */
    data class Parse(
        override val userMessage: String,
        override val cause: Throwable? = null,
    ) : AppError {
        override val retryable: Boolean get() = false
    }

    /** 未归类的本地错误或未知异常。 */
    data class Unknown(
        override val userMessage: String,
        override val cause: Throwable? = null,
    ) : AppError {
        override val retryable: Boolean get() = true
    }
}

/** 返回带「action失败: 」前缀的同类别错误；action 为中文动作名（如「加载用户信息」）。 */
fun AppError.withPrefix(action: String): AppError {
    val message = "${action}失败: $userMessage"
    return when (this) {
        is AppError.Network -> copy(userMessage = message)
        is AppError.Timeout -> copy(userMessage = message)
        is AppError.Auth -> copy(userMessage = message)
        is AppError.Business -> copy(userMessage = message)
        is AppError.Http -> copy(userMessage = message)
        is AppError.Parse -> copy(userMessage = message)
        is AppError.Unknown -> copy(userMessage = message)
    }
}
