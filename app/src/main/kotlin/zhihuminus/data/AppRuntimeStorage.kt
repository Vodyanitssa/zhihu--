package com.zhihuminus.data

import android.content.Context
import androidx.core.content.edit

class AppRuntimeStorage(
    context: Context,
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "app_runtime_state",
        Context.MODE_PRIVATE,
    )

    var lastLaunchTimestamp: Long
        get() = preferences.getLong("last_launch_timestamp", 0L)
        set(value) = preferences.edit { putLong("last_launch_timestamp", value) }
}
