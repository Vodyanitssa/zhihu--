package com.zhihuminus.feature.post.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.materialkolor.ktx.harmonize
import com.zhihuminus.R
import com.zhihuminus.data.VoteUpState
import com.zhihuminus.feature.post.PostEvent
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.ui.article.voteUpNeutralContentDuo3

data class PostBottomBarState(
    val voteUpState: VoteUpState = VoteUpState.Neutral,
    val voteUpCount: Int = 0,
    val isCollected: Boolean = false,
    val commentCount: Int = 0,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostBottomBar(
    postType: PostType,
    state: PostBottomBarState,
    onEvent: (PostEvent) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(
                bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 16.dp,
            ).padding(horizontal = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // 赞同/反对按钮
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 赞同
            AnimatedVisibility(
                visible = state.voteUpState == VoteUpState.Neutral || state.voteUpState == VoteUpState.Up,
            ) {
                val upBgColor by animateColorAsState(
                    targetValue = if (state.voteUpState == VoteUpState.Up) voteUpNeutralContentDuo3() else MaterialTheme.colorScheme.surfaceContainer,
                )
                val upContentColor by animateColorAsState(
                    targetValue = if (state.voteUpState == VoteUpState.Up) Color.White else MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(upBgColor)
                        .clickable {
                            if (postType == PostType.Pin) {
                                onEvent(PostEvent.LikePin)
                            } else {
                                onEvent(PostEvent.VoteUp)
                            }
                        }.padding(6.dp, 8.dp, 12.dp, 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_vote_up_24dp),
                        contentDescription = if (postType == PostType.Pin) "赞" else "赞同",
                        tint = upContentColor,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = state.voteUpCount.toString(),
                        color = upContentColor,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            AnimatedVisibility(visible = postType != PostType.Pin && state.voteUpState == VoteUpState.Neutral) {
                Spacer(modifier = Modifier.width(4.dp))
            }

            // 反对（Pin 不支持）
            AnimatedVisibility(
                visible = postType != PostType.Pin && (state.voteUpState == VoteUpState.Neutral || state.voteUpState == VoteUpState.Down),
            ) {
                val downBgColor by animateColorAsState(
                    targetValue = if (state.voteUpState == VoteUpState.Down) voteUpNeutralContentDuo3() else MaterialTheme.colorScheme.surfaceContainer,
                )
                val downContentColor by animateColorAsState(
                    targetValue = if (state.voteUpState == VoteUpState.Down) Color.White else MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(downBgColor)
                        .clickable { onEvent(PostEvent.VoteDown) }
                        .padding(6.dp, 8.dp, 8.dp, 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedVisibility(visible = state.voteUpState != VoteUpState.Down) {
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_vote_down_24dp),
                        contentDescription = "反对",
                        tint = downContentColor,
                        modifier = Modifier.size(24.dp),
                    )
                    AnimatedVisibility(visible = state.voteUpState == VoteUpState.Down) {
                        Row {
                            Text(
                                text = "反对",
                                color = downContentColor,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        // 操作按钮：收藏、评论、更多
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(end = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            // 收藏
            IconButton(
                onClick = { onEvent(PostEvent.ShowCollectionDialog) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (state.isCollected) {
                        Color(0xFFF57C00).harmonize(MaterialTheme.colorScheme.primary)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    contentColor = if (state.isCollected) {
                        Color.White.copy(alpha = 0.87f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                ),
            ) {
                Icon(
                    if (state.isCollected) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = "收藏",
                )
            }

            Button(
                onClick = { onEvent(PostEvent.Comment) },
                contentPadding = PaddingValues(start = 8.dp, end = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "评论")
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "${state.commentCount}", style = MaterialTheme.typography.titleMedium)
            }

            IconButton(
                onClick = { onEvent(PostEvent.ShowMoreMenu) },
            ) {
                Icon(Icons.Filled.MoreVert, contentDescription = "更多选项")
            }
        }
    }
}
