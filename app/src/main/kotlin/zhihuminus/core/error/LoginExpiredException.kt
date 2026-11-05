package com.zhihuminus.core.error

/** 登录态失效：HTTP 401 且服务端返回 ERR_TICKET_NOT_EXIST。 */
class LoginExpiredException(
    val requestUrl: String,
    cause: Throwable? = null,
) : RuntimeException("Login expired on $requestUrl", cause)
