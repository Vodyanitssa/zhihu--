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

package com.zhihuminus.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.Video

@Composable
fun RenderVideoBox(
    videoId: Long,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
                navigator.onNavigate(Video(videoId))
            },
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = rememberMarkdownImageModel(thumbnailUrl),
                contentDescription = "视频封面",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Surface(
            modifier = Modifier.align(Alignment.Center),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.56f),
            onClick = {
                navigator.onNavigate(Video(videoId))
            },
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "播放视频",
                tint = Color.White,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
