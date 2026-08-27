package com.zhihuminus.feature.question.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.core.content.renderer.RenderContentNodes
import com.zhihuminus.feature.question.QuestionDetail
import com.zhihuminus.feature.question.QuestionEvent
import com.zhihuminus.feature.question.QuestionSort
import com.zhihuminus.feature.question.QuestionTopic
import com.zhihuminus.navigation.Topic
import kotlin.math.roundToInt

private val QUESTION_DETAIL_COLLAPSED_MAX_HEIGHT: Dp = 180.dp
private val QUESTION_DETAIL_MASK_HEIGHT: Dp = 88.dp
private val QUESTION_DETAIL_TOGGLE_ZONE_HEIGHT: Dp = 56.dp

/**
 * 问题标题与统计信息。
 */
@Composable
internal fun QuestionHeaderSection(
    title: String,
    visitCount: Int,
    commentCount: Int,
    followerCount: Int,
    onShowComments: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SelectionContainer {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatItem(icon = Icons.Outlined.Visibility, text = "$visitCount 浏览")
                StatItem(icon = Icons.Outlined.ChatBubbleOutline, text = "$commentCount 评论")
                StatItem(icon = Icons.Outlined.FavoriteBorder, text = "$followerCount 关注")
            }
            OutlinedButton(onClick = onShowComments) {
                Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "评论")
                Spacer(Modifier.width(8.dp))
                Text("$commentCount")
            }
        }
    }
}

/**
 * 问题详情（可折叠）+ 主操作（写回答/关注）+ 回答排序切换。
 */
@Composable
internal fun QuestionBodyHeader(
    questionId: Long,
    detail: QuestionDetail,
    contentNodes: List<ContentNode>,
    allowDetailCollapse: Boolean,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    currentSort: QuestionSort,
    onEvent: (QuestionEvent) -> Unit,
) {
    val density = LocalDensity.current
    val sectionSpacingPx = with(density) { 16.dp.roundToPx() }
    val collapsedViewportHeightPx = with(density) { QUESTION_DETAIL_COLLAPSED_MAX_HEIGHT.roundToPx() }
    var fullViewportHeightPx by remember(questionId) { mutableIntStateOf(0) }
    var animationInitialized by remember(questionId) { mutableStateOf(false) }
    val animatedViewportHeightPx = remember(questionId) { Animatable(collapsedViewportHeightPx.toFloat()) }
    val animationSpec = remember { tween<Float>(durationMillis = 420, easing = FastOutSlowInEasing) }

    LaunchedEffect(questionId) {
        animationInitialized = false
        animatedViewportHeightPx.snapTo(collapsedViewportHeightPx.toFloat())
    }
    LaunchedEffect(isExpanded, fullViewportHeightPx, collapsedViewportHeightPx) {
        if (!allowDetailCollapse || fullViewportHeightPx <= 0) return@LaunchedEffect
        val collapsedTarget = collapsedViewportHeightPx.coerceAtMost(fullViewportHeightPx).toFloat()
        val targetHeight = if (isExpanded) fullViewportHeightPx.toFloat() else collapsedTarget
        if (!animationInitialized) {
            animationInitialized = true
            animatedViewportHeightPx.snapTo(targetHeight)
            return@LaunchedEffect
        }
        if ((animatedViewportHeightPx.value - targetHeight).let { if (it < 0f) -it else it } < 0.5f) {
            return@LaunchedEffect
        }
        animatedViewportHeightPx.animateTo(targetValue = targetHeight, animationSpec = animationSpec)
    }

    val collapsedTargetPx = collapsedViewportHeightPx.coerceAtMost(fullViewportHeightPx).toFloat()
    val expandedRangePx = (fullViewportHeightPx.toFloat() - collapsedTargetPx).coerceAtLeast(1f)
    val expandProgress =
        if (!allowDetailCollapse || fullViewportHeightPx <= 0) {
            1f
        } else {
            ((animatedViewportHeightPx.value - collapsedTargetPx) / expandedRangePx).coerceIn(0f, 1f)
        }
    val viewportHeightPx =
        if (allowDetailCollapse) {
            animatedViewportHeightPx.value.roundToInt()
        } else {
            fullViewportHeightPx
        }

    SubcomposeLayout(modifier = Modifier.fillMaxWidth()) { constraints ->
        val looseConstraints =
            constraints.copy(
                minWidth = 0,
                minHeight = 0,
                maxHeight = Constraints.Infinity,
            )
        val detailPlaceable =
            subcompose("detail") {
                if (contentNodes.isEmpty() && detail.topics.isEmpty()) return@subcompose
                if (allowDetailCollapse) {
                    QuestionDetailAnimatedViewport(
                        questionId = questionId,
                        contentNodes = contentNodes,
                        topics = detail.topics,
                        onTopicClick = { topic -> onEvent(QuestionEvent.Navigate(Topic(id = topic.id, section = topic.name))) },
                        viewportHeightPx = viewportHeightPx,
                        isExpanded = isExpanded,
                        overlayAlpha = 1f - expandProgress,
                        onToggleExpanded = onToggleExpanded,
                        onMeasuredFullHeight = { fullViewportHeightPx = it },
                    )
                } else {
                    QuestionDetailStaticContent(
                        questionId = questionId,
                        contentNodes = contentNodes,
                        topics = detail.topics,
                        onTopicClick = { topic -> onEvent(QuestionEvent.Navigate(Topic(id = topic.id, section = topic.name))) },
                        onMeasuredHeight = { fullViewportHeightPx = it },
                    )
                }
            }.singleOrNull()?.measure(looseConstraints)
        val controlsPlaceable =
            subcompose("controls") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    QuestionPrimaryActions(
                        isFollowing = detail.isFollowing,
                        onFollowClick = { onEvent(QuestionEvent.ToggleFollow) },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${detail.answerCount} 回答",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.weight(1f))
                        SortChip(label = "默认", selected = currentSort == QuestionSort.DEFAULT) {
                            onEvent(QuestionEvent.ChangeSort(QuestionSort.DEFAULT))
                        }
                        SortChip(label = "最新", selected = currentSort == QuestionSort.LATEST) {
                            onEvent(QuestionEvent.ChangeSort(QuestionSort.LATEST))
                        }
                    }
                }
            }.single().measure(looseConstraints)
        val detailHeight = detailPlaceable?.height ?: 0
        val controlsOffset = detailHeight + if (detailPlaceable == null) 0 else sectionSpacingPx
        val totalHeight = controlsOffset + controlsPlaceable.height
        layout(width = constraints.maxWidth, height = totalHeight) {
            detailPlaceable?.place(0, 0)
            controlsPlaceable.place(0, controlsOffset)
        }
    }
}

@Composable
private fun SortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.semantics { this.selected = selected },
        label = { Text(label) },
    )
}

@Composable
private fun QuestionDetailStaticContent(
    questionId: Long,
    contentNodes: List<ContentNode>,
    topics: List<QuestionTopic>,
    onTopicClick: (QuestionTopic) -> Unit,
    onMeasuredHeight: (Int) -> Unit,
) {
    SubcomposeLayout(modifier = Modifier.fillMaxWidth()) { constraints ->
        val placeable =
            subcompose("static_detail") {
                QuestionDetailWithTopics(
                    questionId = questionId,
                    contentNodes = contentNodes,
                    topics = topics,
                    onTopicClick = onTopicClick,
                )
            }.single().measure(
                constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                    maxHeight = Constraints.Infinity,
                ),
            )
        if (placeable.height > 0) {
            onMeasuredHeight(placeable.height)
        }
        layout(width = constraints.maxWidth, height = placeable.height) {
            placeable.place(0, 0)
        }
    }
}

@Composable
private fun QuestionDetailAnimatedViewport(
    questionId: Long,
    contentNodes: List<ContentNode>,
    topics: List<QuestionTopic>,
    onTopicClick: (QuestionTopic) -> Unit,
    viewportHeightPx: Int,
    isExpanded: Boolean,
    overlayAlpha: Float,
    onToggleExpanded: () -> Unit,
    onMeasuredFullHeight: (Int) -> Unit,
) {
    val maskHeightPx = with(LocalDensity.current) { QUESTION_DETAIL_MASK_HEIGHT.roundToPx() }
    val buttonZoneHeightPx = with(LocalDensity.current) { QUESTION_DETAIL_TOGGLE_ZONE_HEIGHT.roundToPx() }
    SubcomposeLayout(
        modifier =
            Modifier
                .fillMaxWidth()
                .clipToBounds(),
    ) { constraints ->
        val looseConstraints =
            constraints.copy(
                minWidth = 0,
                minHeight = 0,
                maxHeight = Constraints.Infinity,
            )
        val contentPlaceable =
            subcompose("content") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = QUESTION_DETAIL_TOGGLE_ZONE_HEIGHT),
                ) {
                    QuestionDetailWithTopics(
                        questionId = questionId,
                        contentNodes = contentNodes,
                        topics = topics,
                        onTopicClick = onTopicClick,
                    )
                }
            }.single().measure(looseConstraints)
        if (contentPlaceable.height > 0) {
            onMeasuredFullHeight(contentPlaceable.height)
        }
        val layoutHeight = viewportHeightPx.coerceIn(0, contentPlaceable.height.coerceAtLeast(0))
        val buttonPlaceable =
            subcompose("button") {
                QuestionDetailToggleButton(
                    isExpanded = isExpanded,
                    onClick = onToggleExpanded,
                )
            }.single().measure(looseConstraints)
        val overlayPlaceable =
            subcompose("overlay") {
                QuestionDetailOverlayMask(
                    alpha = overlayAlpha,
                    modifier = Modifier.fillMaxWidth(),
                )
            }.single().measure(
                Constraints(
                    minWidth = constraints.maxWidth,
                    maxWidth = constraints.maxWidth,
                    minHeight = 0,
                    maxHeight = layoutHeight.coerceAtLeast(0),
                ),
            )
        layout(width = constraints.maxWidth, height = layoutHeight) {
            contentPlaceable.place(0, 0)
            if (overlayAlpha > 0f) {
                overlayPlaceable.place(
                    0,
                    (layoutHeight - minOf(maskHeightPx, overlayPlaceable.height)).coerceAtLeast(0),
                )
            }
            val buttonY =
                (layoutHeight - buttonZoneHeightPx + (buttonZoneHeightPx - buttonPlaceable.height) / 2).coerceAtLeast(0)
            val buttonX = (constraints.maxWidth - buttonPlaceable.width).coerceAtLeast(0)
            buttonPlaceable.place(buttonX, buttonY)
        }
    }
}

@Composable
private fun QuestionDetailWithTopics(
    questionId: Long,
    contentNodes: List<ContentNode>,
    topics: List<QuestionTopic>,
    onTopicClick: (QuestionTopic) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (contentNodes.isNotEmpty()) {
            RenderContentNodes(contentNodes)
        }
        if (topics.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                topics.forEach { topic ->
                    FilterChip(
                        selected = false,
                        onClick = { onTopicClick(topic) },
                        label = { Text("# ${topic.name}") },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionDetailOverlayMask(alpha: Float, modifier: Modifier = Modifier) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    Box(
        modifier =
            modifier
                .height(QUESTION_DETAIL_MASK_HEIGHT)
                .graphicsLayer { this.alpha = alpha }
                .blur(12.dp)
                .background(
                    brush =
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                surfaceColor.copy(alpha = 0.7f),
                                surfaceColor,
                            ),
                        ),
                ),
    )
}

@Composable
private fun QuestionDetailToggleButton(
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier =
            Modifier
                .offset(y = 4.dp)
                .padding(end = 4.dp, bottom = 0.dp),
    ) {
        Icon(
            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
        )
        Spacer(Modifier.width(4.dp))
        Text(if (isExpanded) "收起详情" else "展开详情")
    }
}

@Composable
private fun QuestionPrimaryActions(
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        androidx.compose.material3.FilledTonalButton(
            onClick = onFollowClick,
            modifier =
                Modifier
                    .weight(1f)
                    .semantics {
                        selected = isFollowing
                    },
            colors =
                if (isFollowing) {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                } else {
                    ButtonDefaults.filledTonalButtonColors()
                },
        ) {
            Icon(
                imageVector = if (isFollowing) Icons.Filled.Check else Icons.Filled.Add,
                contentDescription = if (isFollowing) "取消关注" else "关注问题",
            )
            Spacer(Modifier.width(8.dp))
            Text(if (isFollowing) "已关注" else "关注问题")
        }
    }
}

@Composable
private fun StatItem(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
