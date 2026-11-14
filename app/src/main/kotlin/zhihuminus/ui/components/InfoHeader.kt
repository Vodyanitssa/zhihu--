package com.zhihuminus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

private val InfoIconSize = 88.dp

/**
 * 人物、专栏、话题详情页共用的信息卡头部。
 *
 * 固定版式：
 * 1. 第一行：左侧图标，右侧统计信息 + 操作按钮（关注、私信等）；
 * 2. 第二行：名称大标题 + 徽标插槽；
 * 3. 第三行：简介插槽；
 * 4. 其后：各页自己的附加信息。
 *
 * [iconShape] 由调用方决定（头像/话题用圆形、专栏封面用圆角方形）；[actions] 为 null
 * 时不渲染按钮行，避免详情未加载时留下空白。[iconContent] 优先于 [iconUrl]，用于没有
 * 远程封面、需要本地图标或生成式占位的实体（例如收藏夹）。
 */
@Composable
fun InfoHeader(
    iconUrl: String?,
    iconShape: Shape,
    title: String,
    stats: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    iconContent: (@Composable BoxScope.() -> Unit)? = null,
    iconContentDescription: String? = null,
    iconBackgroundColor: Color? = null,
    onIconClick: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    titleBadges: @Composable RowScope.() -> Unit = {},
    introduction: @Composable ColumnScope.() -> Unit = {},
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val rightWidth = maxWidth * 0.6f
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(InfoIconSize)
                        .clip(iconShape)
                        .then(
                            if (iconBackgroundColor != null) {
                                Modifier.background(iconBackgroundColor)
                            } else {
                                Modifier
                            },
                        ).then(
                            if (onIconClick != null) {
                                Modifier.clickable(onClick = onIconClick)
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    if (iconContent != null) {
                        iconContent()
                    } else {
                        AsyncImage(
                            model = iconUrl,
                            contentDescription = iconContentDescription,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Column(
                    modifier = Modifier.width(rightWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        content = stats,
                    )

                    if (actions != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .height(FollowButtonHeight),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            content = actions,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            titleBadges()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            content = introduction,
        )

        footer()
    }
}
