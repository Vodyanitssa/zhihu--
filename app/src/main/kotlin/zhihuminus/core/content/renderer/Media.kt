package com.zhihuminus.core.content.renderer

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.Extras
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.core.error.AppError
import com.zhihuminus.core.error.reportFailure
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.Video

private const val IMAGE_PLACEHOLDER_ASPECT_RATIO = 3f / 2f
private val ImageRetryExtra = Extras.Key(0)

@Composable
private fun Modifier.shimmer(enabled: Boolean): Modifier {
    if (!enabled) return this
    val transition = rememberInfiniteTransition(label = "imageShimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "imageShimmerProgress",
    )
    val highlight = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f)
    return drawWithContent {
        drawContent()
        val bandWidth = size.width * 0.6f
        val startX = -bandWidth + (size.width + bandWidth * 2f) * progress
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, highlight, Color.Transparent),
                start = Offset(startX, 0f),
                end = Offset(startX + bandWidth, size.height),
            ),
        )
    }
}

@Composable
internal fun Image(
    node: ContentNode.Image,
    colorFilter: ColorFilter? = null,
) {
    val imageViewerManager = LocalImageViewManager.current
    val context = LocalContext.current
    var imageState by remember(node.url) {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    var loadError by remember(node.url) { mutableStateOf<AppError?>(null) }
    var retryKey by remember(node.url) { mutableStateOf(0) }
    val isError = imageState is AsyncImagePainter.State.Error
    val isSuccess = imageState is AsyncImagePainter.State.Success
    val imageRequest = remember(node.url, retryKey) {
        ImageRequest
            .Builder(context)
            .data(node.url)
            .apply { extras[ImageRetryExtra] = retryKey }
            .build()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ).then(if (isSuccess) Modifier else Modifier.aspectRatio(IMAGE_PLACEHOLDER_ASPECT_RATIO))
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSuccess) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant)
                .shimmer(enabled = imageState is AsyncImagePainter.State.Loading)
                .clickable {
                    if (isError) {
                        if (loadError?.retryable != false) {
                            loadError = null
                            imageState = AsyncImagePainter.State.Empty
                            retryKey++
                        }
                    } else {
                        imageViewerManager?.show(node.url)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = node.caption,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
                colorFilter = colorFilter,
                onState = { state ->
                    imageState = state
                    if (state is AsyncImagePainter.State.Error) {
                        loadError = reportFailure(
                            tag = "ComposeRenderer",
                            logAction = "load article image",
                            error = state.result.throwable,
                        )
                    }
                },
            )
            if (isError) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp),
                    )
                    Text(
                        text = "图片加载失败",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (loadError?.retryable != false) {
                        Text(
                            text = "点击重试",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
    node.caption?.let {
        val effectiveStyle = scaledContentStyle(
            MaterialTheme.typography.bodySmall,
            maxLineHeight = 1.3f,
        )
        Text(
            text = it,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = effectiveStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun Video(node: ContentNode.Video) {
    val navigator = LocalNavigator.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            contentAlignment = Alignment.Center,
        ) {
            val coverUrl = node.coverUrl
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = node.caption,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }

            IconButton(
                onClick = { navigator.onNavigate(Video(node.videoId)) },
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        node.caption?.let { caption ->
            val effectiveStyle = scaledContentStyle(
                MaterialTheme.typography.bodySmall,
                maxLineHeight = 1.3f,
            )
            Text(
                text = caption,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = effectiveStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
