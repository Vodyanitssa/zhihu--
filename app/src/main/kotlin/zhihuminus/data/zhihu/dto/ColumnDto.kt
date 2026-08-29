package com.zhihuminus.data.zhihu.dto

import kotlinx.serialization.Serializable

@Serializable
data class ColumnArticlePage(
    val paging: ColumnPaging,
    val data: List<ColumnArticleDto>,
)

@Serializable
data class ColumnPaging(
    val isEnd: Boolean = false,
    val isStart: Boolean = false,
    val totals: Int = 0,
    val previous: String? = null,
    val next: String? = null,
)

@Serializable
data class ColumnArticleDto(
    val id: String,
    val title: String,
    val titleImage: String? = null,
    val url: String,
    val excerpt: String? = null,
    val content: String? = null,
    val author: AuthorDto? = null,
    val created: Long = 0,
    val updated: Long = 0,
    val voteupCount: Int = 0,
    val commentCount: Int = 0,
    val articleType: String? = null,
    val hasColumn: Boolean = false,
)
