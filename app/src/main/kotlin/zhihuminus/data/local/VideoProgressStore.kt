package com.zhihuminus.data.local

import android.content.Context
import androidx.core.content.edit

interface VideoProgressStore {
    fun getProgress(videoId: Long): Long

    fun saveProgress(videoId: Long, position: Long)

    fun removeProgress(videoId: Long)
}

class SharedPreferencesVideoProgressStore(
    context: Context,
) : VideoProgressStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        "video_playback_progress",
        Context.MODE_PRIVATE,
    )

    override fun getProgress(videoId: Long): Long {
        if (videoId == 0L) return 0L
        return preferences.getLong("progress_$videoId", 0L)
    }

    override fun saveProgress(videoId: Long, position: Long) {
        if (videoId == 0L) return
        preferences.edit { putLong("progress_$videoId", position) }
    }

    override fun removeProgress(videoId: Long) {
        if (videoId == 0L) return
        preferences.edit { remove("progress_$videoId") }
    }
}

fun VideoProgressStore(context: Context): VideoProgressStore =
    SharedPreferencesVideoProgressStore(context)
