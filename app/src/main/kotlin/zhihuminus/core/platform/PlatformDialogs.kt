package com.zhihuminus.core.platform

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.zhihuminus.core.util.HttpStatusException
import com.zhihuminus.data.ZhihuJson.json
import com.zhihuminus.platform.UserMessageSink
import com.zhihuminus.platform.androidUserMessageSink
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun Context.canSafelyShowDialog(): Boolean {
    val activity = this as? Activity ?: return false
    if (activity.isFinishing || activity.isDestroyed) return false
    val lifecycleOwner = activity as? LifecycleOwner ?: return true
    return lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
}

fun Context.tryShowLoginExpiredDialog(
    error: HttpStatusException,
    onRelogin: () -> Unit,
): Boolean {
    return try {
        val body = json.parseToJsonElement(error.bodyText).jsonObject
        val errorBody = body["error"]?.jsonObject ?: return false
        if (errorBody["code"]?.jsonPrimitive?.int == 100 &&
            errorBody["message"]?.jsonPrimitive?.content == "ERR_TICKET_NOT_EXIST"
        ) {
            mainExecutor.execute {
                if (canSafelyShowDialog()) {
                    AlertDialog
                        .Builder(this)
                        .setTitle("登录已过期")
                        .setMessage("请重新登录以继续使用完整功能。")
                        .setPositiveButton("重新登录") { _, _ -> onRelogin() }
                        .setNegativeButton("取消", null)
                        .show()
                }
            }
            true
        } else {
            false
        }
    } catch (_: Exception) {
        false
    }
}

fun Context.showDebugErrorDialog(
    error: HttpStatusException,
    messageSink: UserMessageSink = androidUserMessageSink(this),
) {
    mainExecutor.execute {
        if (!canSafelyShowDialog()) return@execute
        AlertDialog
            .Builder(this)
            .setTitle("错误 ${error.status}")
            .setMessage(error.bodyText)
            .setNeutralButton("复制curl") { _, _ ->
                copyPlainText("curl", error.dumpedCurlRequest.orEmpty())
                messageSink.showShortMessage("已复制到剪贴板")
            }.show()
    }
}
