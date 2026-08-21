/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zhihuminus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.zhihuminus.platform.androidSettingsStore

class VideoPlayerActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private var videoId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val videoUrl = intent.getStringExtra("video_url")
        if (videoUrl.isNullOrBlank()) {
            finish()
            return
        }

        videoId = intent.getLongExtra("video_id", 0L)

        enableEdgeToEdge()
        enterFullscreen()

        val savedPosition = loadSavedPosition()

        player = ExoPlayer
            .Builder(this)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(videoUrl))

                if (savedPosition > 0L) {
                    seekTo(savedPosition)
                }

                prepare()
                playWhenReady = true
            }

        setContent {
            VideoPlayerScreen(
                player = player!!,
                onBack = ::finish,
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        // 用户通过手势临时呼出系统栏后，再次获得焦点时重新隐藏。
        if (hasFocus) {
            enterFullscreen()
        }
    }

    override fun onPause() {
        saveCurrentProgress()
        super.onPause()
    }

    override fun onDestroy() {
        saveCurrentProgress()
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun enterFullscreen() {
        val controller = WindowInsetsControllerCompat(
            window,
            window.decorView,
        )

        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun loadSavedPosition(): Long {
        if (videoId == 0L) {
            return 0L
        }

        return androidSettingsStore(this)
            .getLong("video_progress_$videoId", 0L)
    }

    private fun saveCurrentProgress() {
        val currentPlayer = player ?: return

        if (videoId == 0L) {
            return
        }

        try {
            val position = currentPlayer.currentPosition
            val duration = currentPlayer.duration

            if (position <= 1000L || duration <= 0L) {
                return
            }

            val store = androidSettingsStore(this)

            if (duration - position <= 3000L) {
                store.remove("video_progress_$videoId")
            } else {
                store.putLong("video_progress_$videoId", position)
            }
        } catch (_: IllegalStateException) {
            // Player 已经释放，忽略。
        }
    }
}

@Composable
private fun VideoPlayerScreen(
    player: ExoPlayer,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        VideoPlayer(
            player = player,
        )

        BackButton(
            onClick = onBack,
        )
    }
}

@Composable
private fun BackButton(
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(12.dp)
            .size(40.dp)
            .background(
                color = Color.Black.copy(alpha = 0.55f),
                shape = CircleShape,
            ),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "返回",
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(
    player: ExoPlayer,
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            PlayerView(context).apply {
                this.player = player

                useController = true
                controllerAutoShow = true
                controllerHideOnTouch = true
                controllerShowTimeoutMs = 3000
            }
        },
        update = { view ->
            view.player = player
        },
    )
}
