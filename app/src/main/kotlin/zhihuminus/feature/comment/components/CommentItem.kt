package com.zhihuminus.feature.comment.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.zhihuminus.core.content.AstParser
import com.zhihuminus.core.content.renderer.ContentNodes
import com.zhihuminus.core.util.formatDateTime
import com.zhihuminus.feature.comment.Comment
import com.zhihuminus.feature.comment.CommentEvent

/**
 * 通用评论条目组件，只负责渲染单条评论。
 *
 * @param comment 评论数据
 * @param onEvent 统一事件回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentItem(
    comment: Comment,
    modifier: Modifier = Modifier,
    onEvent: (CommentEvent) -> Unit = {},
) {
    var showMoreMenu by remember(comment.id) { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // 作者信息 + 内容
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.padding(top = 4.dp)) {
                AsyncImage(
                    model = comment.author.avatarUrl,
                    contentDescription = "头像",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.author.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.alignByBaseline(),
                    )

                    comment.authorTag?.let { tag ->
                        Spacer(modifier = Modifier.width(4.dp))
                        AuthorTag(tag)
                    }

                    comment.replyToAuthor?.let { replyTo ->
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "回复",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.alignByBaseline(),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = replyTo.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .widthIn(max = 120.dp)
                                .alignByBaseline(),
                        )
                    }
                }

                SelectionContainer {
                    val contentNodes = remember(comment.content) {
                        AstParser.parseContent(comment.content)
                    }
                    ContentNodes(contentNodes)
                }
            }
        }

        // 底部操作栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // 时间
            val formattedTime = remember(comment.createdAt) {
                formatDateTime(comment.createdAt)
            }
            Text(
                text = formattedTime,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alignByBaseline(),
            )

            // IP 属地
            comment.commentTags.firstOrNull()?.let { tag ->
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tag,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 删除
            if (comment.canDelete) {
                Box {
                    IconButton(
                        onClick = { showMoreMenu = true },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "更多操作",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                onEvent(CommentEvent.DeleteComment(comment.id))
                            },
                        )
                    }
                }
            }

            // 回复
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onEvent(CommentEvent.Reply(comment)) },
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Comment,
                    contentDescription = "回复",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 点赞
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    if (comment.liked) {
                        onEvent(CommentEvent.UnlikeComment(comment.id))
                    } else {
                        onEvent(CommentEvent.LikeComment(comment.id))
                    }
                },
            ) {
                Icon(
                    if (comment.liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    contentDescription = "点赞",
                    modifier = Modifier.size(16.dp),
                    tint = if (comment.liked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = comment.likeCount.toString(),
                    fontSize = 12.sp,
                    color = if (comment.liked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun AuthorTag(authorTag: String) {
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .clip(RoundedCornerShape(3.dp)),
    ) {
        Text(
            text = authorTag,
            style = MaterialTheme.typography.labelSmall,
            lineHeight = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
