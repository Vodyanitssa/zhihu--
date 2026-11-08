package com.zhihuminus.feature.column

import com.zhihuminus.data.FeedDisplayItem

data class ColumnAuthor(
    val id: String = "",
    val urlToken: String = "",
    val name: String = "",
    val avatarUrl: String? = null,
    val headline: String = "",
    val isFollowing: Boolean? = null,
)

data class ColumnDetail(
    val id: String,
    val title: String = "",
    val description: String = "",
    val avatarUrl: String? = null,
    val followerCount: Int = 0,
    val articleCount: Int = 0,
    val voteupCount: Int = 0,
    val isFollowing: Boolean = false,
    val author: ColumnAuthor? = null,
)

interface ColumnRepository {
    /**
     * 获取专栏文章列表
     * @param columnId 专栏 ID
     * @param nextUrl 分页续页 URL（为 null 时从第一页开始）
     * @return 文章列表 + 分页信息
     */
    suspend fun getColumnArticles(columnId: String, nextUrl: String?): ColumnArticleResult

    suspend fun getColumnDetail(columnId: String): ColumnDetail

    suspend fun setFollowing(columnId: String, following: Boolean)

    suspend fun setAuthorFollowing(urlToken: String, following: Boolean)
}

data class ColumnArticleResult(
    val articles: List<FeedDisplayItem>,
    val nextUrl: String?,
    val isEnd: Boolean,
    val totals: Int,
)
