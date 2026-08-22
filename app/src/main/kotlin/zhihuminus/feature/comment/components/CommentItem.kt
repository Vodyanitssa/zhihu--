package com.zhihuminus.feature.comment.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.zhihuminus.core.renderer.ContentNodes
import com.zhihuminus.core.util.formatDateTime
import com.zhihuminus.feature.comment.Comment

/**
 * 通用评论条目组件。
 *
 * @param comment 评论数据
 * @param showSubComments 是否显示子评论预览（仅根评论列表使用）
 * @param onLike 点赞/取消点赞回调
 * @param onReply 回复回调
 * @param onDelete 删除回调
 * @param onChildCommentsClick 查看子评论回调
 * @param onChildLike 子评论点赞回调（用于子评论预览）
 * @param onChildDelete 子评论删除回调（用于子评论预览）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentItem(
    comment: Comment,
    modifier: Modifier = Modifier,
    showSubComments: Boolean = true,
    onLike: (() -> Unit)? = null,
    onReply: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onChildCommentsClick: (() -> Unit)? = null,
    onChildLike: ((Comment) -> Unit)? = null,
    onChildDelete: ((Comment) -> Unit)? = null,
) {
    var showMoreMenu by remember(comment.id) { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // 作者信息 + 内容
        Row(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = comment.author.avatarUrl,
                contentDescription = "头像",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.author.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
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
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = replyTo.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 120.dp),
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
            )

            // IP 属地
            comment.commentTags.firstOrNull()?.let { tag ->
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tag,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 删除
            if (comment.canDelete && onDelete != null) {
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
                                onDelete()
                            },
                        )
                    }
                }
            }

            // 回复/子评论
            if (onReply != null || onChildCommentsClick != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (comment.childCommentCount > 0) {
                            onChildCommentsClick?.invoke()
                        } else {
                            onReply?.invoke()
                        }
                    },
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Comment,
                        contentDescription = "回复",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (comment.childCommentCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = comment.childCommentCount.toString(),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 点赞
            if (onLike != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLike() },
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

        // 子评论预览（仅根评论列表使用）
        if (showSubComments && comment.childCommentCount > 0 && comment.childComments.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, top = 8.dp),
            ) {
                comment.childComments.take(2).forEach { child ->
                    CommentItem(
                        comment = child,
                        showSubComments = false,
                        onLike = onChildLike?.let { cb -> { cb(child) } },
                        onDelete = onChildDelete?.let { cb -> { cb(child) } },
                    )
                }
                if (comment.childCommentCount > 2 && onChildCommentsClick != null) {
                    Button(
                        onClick = onChildCommentsClick,
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Comment,
                            contentDescription = "查看子评论",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "查看 ${comment.childCommentCount} 条子评论",
                            fontSize = 12.sp,
                        )
                    }
                }
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
            fontSize = 12.sp,
            lineHeight = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
