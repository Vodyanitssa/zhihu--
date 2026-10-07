package com.zhihuminus.feature.search

import com.zhihuminus.data.DataHolder
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.feature.people.PeopleMemberItem
import kotlinx.serialization.Serializable

const val ZHIHU_HOT_SEARCH_URL = "https://www.zhihu.com/api/v4/search/hot_search"

enum class SearchTab(
    val label: String,
) {
    General("全站"),
    People("用户"),
    Topic("话题"),
}

enum class SearchSortOption(
    val label: String,
    val value: String,
) {
    Default("综合排序", ""),
    Latest("最新发布", "created_time"),
    MostVoted("最多赞同", "upvoted_count"),
}

enum class SearchContentType(
    val label: String,
    val value: String,
) {
    All("全部内容", ""),
    Answer("回答", "answer"),
    Article("文章", "article"),
    Video("视频", "zvideo"),
}

enum class SearchTimeRange(
    val label: String,
    val value: String,
) {
    All("不限时间", ""),
    Day("一天内", "a_day"),
    Week("一周内", "a_week"),
    Month("一个月内", "a_month"),
    ThreeMonths("三个月内", "three_months"),
    HalfYear("半年内", "half_a_year"),
    Year("一年内", "a_year"),
}

data class TopicSearchResult(
    val topic: DataHolder.Topic,
    val excerpt: String,
    val visitCount: Long,
    val discussCount: Long,
    val isFollowing: Boolean,
)

data class PeopleSearchResult(
    val people: PeopleMemberItem,
    val highlightedName: String,
)

@Serializable
data class HotSearchItem(
    val query: String,
    val hotShow: String = "",
    val label: String = "",
)

data class SearchPage<T>(
    val items: List<T>,
    val nextUrl: String?,
    val isEnd: Boolean,
)

interface SearchRepository {
    suspend fun searchGeneral(
        query: String,
        tab: SearchTab,
        sort: SearchSortOption,
        contentType: SearchContentType,
        timeRange: SearchTimeRange,
        restrictedMemberHashId: String,
        nextUrl: String?,
    ): SearchPage<FeedDisplayItem>

    suspend fun searchPeople(
        query: String,
        sort: SearchSortOption,
        timeRange: SearchTimeRange,
        restrictedMemberHashId: String,
        nextUrl: String?,
    ): SearchPage<PeopleSearchResult>

    suspend fun searchTopics(
        query: String,
        sort: SearchSortOption,
        timeRange: SearchTimeRange,
        restrictedMemberHashId: String,
        nextUrl: String?,
    ): SearchPage<TopicSearchResult>

    suspend fun fetchHotSearches(): List<HotSearchItem>

    fun isHotSearchEnabled(): Boolean

    fun isSearchHistoryEnabled(): Boolean

    suspend fun setTopicFollowing(
        topicId: String,
        following: Boolean,
    ): Result<Unit>

    suspend fun setMemberFollowing(
        urlToken: String,
        following: Boolean,
    ): Result<Unit>

    fun getSearchHistory(): List<String>

    fun saveSearchHistory(history: List<String>)

    fun clearSearchHistory()
}
