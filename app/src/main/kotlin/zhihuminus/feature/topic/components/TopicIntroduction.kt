package com.zhihuminus.feature.topic.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun TopicIntroduction(
    introduction: String,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var canCollapse by remember(introduction) { mutableStateOf(false) }
    val surfaceColor = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clipToBounds()
            .animateContentSize(
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            ),
    ) {
        Text(
            text = introduction,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (isExpanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (canCollapse) Modifier.padding(bottom = 56.dp) else Modifier),
            onTextLayout = { result ->
                canCollapse = result.lineCount > 3 || result.hasVisualOverflow
            },
        )
        if (canCollapse && !isExpanded) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(88.dp)
                    .blur(12.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                surfaceColor.copy(alpha = 0.7f),
                                surfaceColor,
                            ),
                        ),
                    ),
            )
        }
        if (canCollapse) {
            TextButton(
                onClick = { onExpandedChange(!isExpanded) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(y = 4.dp)
                    .padding(end = 4.dp),
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                )
                Spacer(Modifier.width(4.dp))
                Text(if (isExpanded) "收起简介" else "展开简介")
            }
        }
    }
}
