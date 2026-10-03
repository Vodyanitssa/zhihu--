package com.zhihuminus.core.platform

import android.app.Activity
import android.content.Context
import android.content.Intent

fun Context.startLoginActivity() {
    val intent = Intent().setClassName(packageName, "com.zhihuminus.LoginActivity")
    if (this !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}

fun Context.restartApplication() {
    val activity = this as? Activity ?: return
    val launchIntent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
        ?: error("无法获取应用启动入口")
    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    activity.startActivity(launchIntent)
}
