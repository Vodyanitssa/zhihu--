package com.zhihuminus.data.zhihu.dto

import kotlinx.serialization.Serializable

@Serializable
data class ColumnArticlePage(
    val paging: PagingDto = PagingDto(),
    val data: List<ColumnArticleDto>,
)

@Serializable
data class ColumnDetailDto(
    val id: String = "",
    val title: String = "",
    val intro: String? = null,
    val excerpt: String? = null,
    val imageUrl: String? = null,
    val followers: Int = 0,
    val articlesCount: Int = 0,
    val itemsCount: Int = 0,
    val isFollowing: Boolean = false,
    val voteupCount: Int = 0,
    val updated: Long = 0,
    val created: Long = 0,
    val author: AuthorDto? = null,
)

@Serializable
data class ColumnArticleDto(
    val id: String,
    val title: String,
    val titleImage: String? = null,
    val url: String,
    val excerpt: String? = null,
    val excerptTitle: String? = null,
    val content: String? = null,
    val author: AuthorDto? = null,
    val created: Long = 0,
    val updated: Long = 0,
    val voteupCount: Int = 0,
    val commentCount: Int = 0,
    val articleType: String? = null,
    val hasColumn: Boolean = false,
)
