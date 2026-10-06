package com.zhihuminus.data

import android.content.Context
import androidx.core.content.edit

class VideoProgressStorage(
    context: Context,
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "video_playback_progress",
        Context.MODE_PRIVATE,
    )

    fun getProgress(videoId: Long): Long {
        if (videoId == 0L) return 0L
        return preferences.getLong("progress_$videoId", 0L)
    }

    fun saveProgress(videoId: Long, position: Long) {
        if (videoId == 0L) return
        preferences.edit { putLong("progress_$videoId", position) }
    }

    fun removeProgress(videoId: Long) {
        if (videoId == 0L) return
        preferences.edit { remove("progress_$videoId") }
    }
}
