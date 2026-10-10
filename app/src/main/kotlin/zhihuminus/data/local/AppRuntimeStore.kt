package com.zhihuminus.data.local

import android.content.Context
import androidx.core.content.edit

interface AppRuntimeStore {
    var lastLaunchTimestamp: Long
}

class SharedPreferencesAppRuntimeStore(
    context: Context,
) : AppRuntimeStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        "app_runtime_state",
        Context.MODE_PRIVATE,
    )

    override var lastLaunchTimestamp: Long
        get() = preferences.getLong("last_launch_timestamp", 0L)
        set(value) = preferences.edit { putLong("last_launch_timestamp", value) }
}

fun AppRuntimeStore(context: Context): AppRuntimeStore =
    SharedPreferencesAppRuntimeStore(context)
