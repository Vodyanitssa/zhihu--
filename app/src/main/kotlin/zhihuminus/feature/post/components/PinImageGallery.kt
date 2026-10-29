package com.zhihuminus.feature.post.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.core.content.renderer.RenderContentNode

internal enum class PinGalleryMode {
    NONE,
    SINGLE,
    STRIP,
}

internal fun pinGalleryMode(imageCount: Int): PinGalleryMode = when {
    imageCount <= 0 -> PinGalleryMode.NONE
    imageCount == 1 -> PinGalleryMode.SINGLE
    else -> PinGalleryMode.STRIP
}

internal fun pinImageAspectRatio(width: Float, height: Float): Float? {
    if (!width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f) return null
    val ratio = width / height
    return ratio.takeIf { it.isFinite() && it in MIN_ASPECT_RATIO..MAX_ASPECT_RATIO }
}

internal fun pinThumbnailWidth(barHeight: Dp, aspectRatio: Float?): Dp {
    val ratio = aspectRatio ?: DEFAULT_ASPECT_RATIO
    return (barHeight.value * ratio).coerceIn(MIN_THUMBNAIL_WIDTH_DP, MAX_THUMBNAIL_WIDTH_DP).dp
}

@Composable
internal fun PinImageGallery(
    images: List<ContentNode.Image>,
    onImageClick: (String) -> Unit,
) {
    when (pinGalleryMode(images.size)) {
        PinGalleryMode.NONE -> Unit
        PinGalleryMode.SINGLE -> RenderContentNode(images.single())
        PinGalleryMode.STRIP -> LazyRow(
            modifier = Modifier
                .padding(top = 8.dp)
                .height(BAR_HEIGHT),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(
                items = images,
                key = { index, image -> "$index:${image.url}" },
            ) { _, image ->
                PinGalleryThumbnail(
                    image = image,
                    onClick = onImageClick,
                )
            }
        }
    }
}

@Composable
private fun PinGalleryThumbnail(
    image: ContentNode.Image,
    onClick: (String) -> Unit,
) {
    var aspectRatio by remember(image.url) { mutableStateOf<Float?>(null) }
    Box(
        modifier = Modifier
            .height(BAR_HEIGHT)
            .width(pinThumbnailWidth(BAR_HEIGHT, aspectRatio))
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick(image.url) },
    ) {
        AsyncImage(
            model = image.url,
            contentDescription = image.caption ?: "想法图片",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            onSuccess = { state ->
                val size = state.painter.intrinsicSize
                pinImageAspectRatio(size.width, size.height)?.let { aspectRatio = it }
            },
        )
    }
}

private val BAR_HEIGHT = 200.dp
private const val MIN_THUMBNAIL_WIDTH_DP = 80f
private const val MAX_THUMBNAIL_WIDTH_DP = 300f
private const val DEFAULT_ASPECT_RATIO = 0.75f
private const val MIN_ASPECT_RATIO = 0.1f
private const val MAX_ASPECT_RATIO = 10f
