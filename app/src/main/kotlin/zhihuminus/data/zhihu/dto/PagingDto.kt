package com.zhihuminus.data.zhihu.dto

import kotlinx.serialization.Serializable

/**
 * 统一的知乎分页游标元数据 DTO。
 * 覆盖 Zhihu 常见的 paging 结构字段（推荐流、专栏、话题、评论、通知、搜索、收藏夹等）。
 */
@Serializable
data class PagingDto(
    val isEnd: Boolean = true,
    val next: String? = null,
    val previous: String? = null,
    val isStart: Boolean = false,
    val totals: Int = 0,
    val page: Int = -1,
) {
    /** 是否有更多数据 */
    val hasMore: Boolean
        get() = !isEnd && !next.isNullOrBlank()

    /** 归一化后的下一页请求 URL，若无下一页则为 null */
    val nextUrl: String?
        get() = next?.takeIf { !isEnd && it.isNotBlank() }
}

/**
 * 统一的一页分页数据结果封装。
 */
data class PageResult<T>(
    val items: List<T>,
    val nextUrl: String?,
    val isEnd: Boolean,
)

typealias FeedPage = PageResult<FeedDto>
typealias HistoryPage = PageResult<HistoryItemDto>
typealias PeoplePageDto<T> = PageResult<T>

@Deprecated("Use PagingDto instead", ReplaceWith("PagingDto"))
typealias ZhihuPaging = PagingDto

@Deprecated("Use PagingDto instead", ReplaceWith("PagingDto"))
typealias ColumnPaging = PagingDto

@Deprecated("Use PagingDto instead", ReplaceWith("PagingDto"))
typealias TopicPagingDto = PagingDto
