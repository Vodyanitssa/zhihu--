package com.zhihuminus.core.error

import com.zhihuminus.core.util.Log
import kotlin.coroutines.cancellation.CancellationException

/**
 * 统一的失败上报入口：透传协程取消、写入日志并返回领域错误模型。
 *
 * @param tag 日志标签，通常为类名。
 * @param logAction 日志动作（英文，如 "load member profile"），生成 "Failed to $logAction"。
 * @param error 捕获到的异常。
 * @param userAction 需要面向用户展示的中文动作名；传入后错误文案会带「$userAction失败: 」前缀。
 */
fun reportFailure(
    tag: String,
    logAction: String,
    error: Throwable,
    userAction: String? = null,
): AppError {
    if (error is CancellationException) throw error
    Log.e(tag, "Failed to $logAction", error)
    val appError = error.toAppError()
    if (appError is AppError.Auth) {
        LoginExpiredEvents.notifyLoginExpired()
    }
    return if (userAction != null) appError.withPrefix(userAction) else appError
}
