package com.zhihuminus.core.error

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * 全局登录过期事件：由 [reportFailure] 在识别出 [AppError.Auth] 时发出，
 * 主壳收集后弹出统一的「重新登录」对话框，避免各页面各自处理。
 */
object LoginExpiredEvents {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events

    fun notifyLoginExpired() {
        _events.tryEmit(Unit)
    }
}
