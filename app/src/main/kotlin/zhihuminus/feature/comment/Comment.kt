package com.zhihuminus.feature.comment

/**
 * 评论领域模型
 */
data class Comment(
    val id: String,
    val content: String,
    val author: CommentAuthor,
    val createdAt: Long,
    val likeCount: Int,
    val liked: Boolean,
    val canDelete: Boolean,
    val isAuthor: Boolean,
    val childCommentCount: Int,
    val childComments: List<Comment>,
    val replyToAuthor: CommentAuthor?,
    val commentTags: List<String>,
    val authorTag: String?,
    /** 该评论所回复的根评论 ID；自身即根评论时为 null */
    val replyRootCommentId: String? = null,
)

/**
 * 评论作者
 */
data class CommentAuthor(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val urlToken: String,
    val headline: String,
)

/**
 * 评论分页结果
 */
data class CommentPage(
    val comments: List<Comment>,
    val isEnd: Boolean,
    val nextUrl: String? = null,
)
