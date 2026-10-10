package com.zhihuminus.data.zhihu

import com.zhihuminus.core.environment.PaginationEnvironment
import com.zhihuminus.core.environment.deleteSigned
import com.zhihuminus.core.environment.postSigned
import com.zhihuminus.core.settings.AppSettingsRepository
import com.zhihuminus.core.util.raiseForStatus
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.ZhihuPaging
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.flattenFeeds
import com.zhihuminus.data.local.SearchHistoryStore
import com.zhihuminus.data.toDisplayItem
import com.zhihuminus.data.zhihu.dto.CommonFeedDto
import com.zhihuminus.data.zhihu.dto.FeedTargetDto
import com.zhihuminus.data.zhihu.dto.MemberItemDto
import com.zhihuminus.data.zhihu.dto.SearchItemDto
import com.zhihuminus.data.zhihu.dto.TopicSearchDto
import com.zhihuminus.feature.search.HotSearchItem
import com.zhihuminus.feature.search.PeopleSearchResult
import com.zhihuminus.feature.search.SearchContentType
import com.zhihuminus.feature.search.SearchPage
import com.zhihuminus.feature.search.SearchRepository
import com.zhihuminus.feature.search.SearchSortOption
import com.zhihuminus.feature.search.SearchTab
import com.zhihuminus.feature.search.SearchTimeRange
import com.zhihuminus.feature.search.SearchTopicItem
import com.zhihuminus.feature.search.TopicSearchResult
import com.zhihuminus.feature.search.ZHIHU_HOT_SEARCH_URL
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private const val SEARCH_INCLUDE = "data[*].highlight,object,type"
private const val SEARCH_VERTICAL_INFO = "0,0,0,0,0,0,0,0,0,0,0,0"

class ZhihuSearchRepository(
    private val environment: PaginationEnvironment,
    private val settingsRepository: AppSettingsRepository,
    private val historyStorage: SearchHistoryStore,
) : SearchRepository {
    override suspend fun searchGeneral(
        query: String,
        tab: SearchTab,
        sort: SearchSortOption,
        contentType: SearchContentType,
        timeRange: SearchTimeRange,
        restrictedMemberHashId: String,
        nextUrl: String?,
    ): SearchPage<FeedDisplayItem> {
        val url = nextUrl ?: buildSearchUrl(query, tab, sort, contentType, timeRange, restrictedMemberHashId)
        val json = environment.fetchJson(url, SEARCH_INCLUDE) ?: error("搜索响应为空")
        val jsonArray = json["data"] as? JsonArray ?: error("搜索响应缺少 data 列表")

        val results = jsonArray.mapNotNull { element ->
            try {
                ZhihuJson.decodeJson<SearchItemDto>(element)
            } catch (e: Exception) {
                environment.logDecodeFailure("ZhihuSearchRepository", element, e)
                null
            }
        }
        val feeds = results.mapNotNull { item ->
            if (item.type != "search_result") return@mapNotNull null
            val obj = item.obj ?: return@mapNotNull null
            val type = (obj as? JsonObject)?.get("type")?.jsonPrimitive?.contentOrNull
            if (type == "people" || type == "topic") return@mapNotNull null
            try {
                val target = ZhihuJson.decodeJson<FeedTargetDto>(obj)
                CommonFeedDto(
                    id = item.id,
                    verb = "SEARCH_RESULT",
                    target = target,
                )
            } catch (e: Exception) {
                environment.logDecodeFailure("ZhihuSearchRepository", obj, e)
                null
            }
        }
        val displayItems = feeds.flattenFeeds().map { it.toDisplayItem() }
        val paging = (json["paging"] as? JsonObject)?.let {
            runCatching { ZhihuJson.decodeJson<ZhihuPaging>(it) }.getOrNull()
        }

        return SearchPage(
            items = displayItems,
            nextUrl = paging?.next,
            isEnd = paging?.isEnd ?: (paging?.next == null),
        )
    }

    override suspend fun searchPeople(
        query: String,
        sort: SearchSortOption,
        timeRange: SearchTimeRange,
        restrictedMemberHashId: String,
        nextUrl: String?,
    ): SearchPage<PeopleSearchResult> {
        val url = nextUrl ?: buildSearchUrl(
            query,
            SearchTab.People,
            sort,
            SearchContentType.All,
            timeRange,
            restrictedMemberHashId,
        )
        val json = environment.fetchJson(url, SEARCH_INCLUDE) ?: error("用户搜索响应为空")
        val jsonArray = json["data"] as? JsonArray ?: error("用户搜索响应缺少 data 列表")

        val results = jsonArray.mapNotNull { element ->
            try {
                ZhihuJson.decodeJson<SearchItemDto>(element)
            } catch (e: Exception) {
                environment.logDecodeFailure("ZhihuSearchRepository", element, e)
                null
            }
        }
        val people = results.mapNotNull { item ->
            if (item.type != "search_result") return@mapNotNull null
            val obj = item.obj ?: return@mapNotNull null
            val type = (obj as? JsonObject)?.get("type")?.jsonPrimitive?.contentOrNull
            if (type != "people") return@mapNotNull null
            try {
                val member = ZhihuJson.decodeJson<MemberItemDto>(obj)
                val cleanName = member.name
                    .replace("<em>", "")
                    .replace("</em>", "")
                PeopleSearchResult(
                    people = member.toPeopleMemberItem().copy(name = cleanName),
                    highlightedName = member.name,
                )
            } catch (e: Exception) {
                environment.logDecodeFailure("ZhihuSearchRepository", obj, e)
                null
            }
        }
        val paging = (json["paging"] as? JsonObject)?.let {
            runCatching { ZhihuJson.decodeJson<ZhihuPaging>(it) }.getOrNull()
        }

        return SearchPage(
            items = people,
            nextUrl = paging?.next,
            isEnd = paging?.isEnd ?: (paging?.next == null),
        )
    }

    override suspend fun searchTopics(
        query: String,
        sort: SearchSortOption,
        timeRange: SearchTimeRange,
        restrictedMemberHashId: String,
        nextUrl: String?,
    ): SearchPage<TopicSearchResult> {
        val url = nextUrl ?: buildSearchUrl(
            query,
            SearchTab.Topic,
            sort,
            SearchContentType.All,
            timeRange,
            restrictedMemberHashId,
        )
        val json = environment.fetchJson(url, SEARCH_INCLUDE) ?: error("话题搜索响应为空")
        val jsonArray = json["data"] as? JsonArray ?: error("话题搜索响应缺少 data 列表")

        val topics = jsonArray.mapNotNull { element ->
            decodeTopicSearchResult(element).also { result ->
                if (result == null) {
                    environment.logDecodeFailure(
                        "ZhihuSearchRepository",
                        element,
                        IllegalArgumentException("话题搜索结果缺少可用的话题对象"),
                    )
                }
            }
        }
        val paging = (json["paging"] as? JsonObject)?.let {
            runCatching { ZhihuJson.decodeJson<ZhihuPaging>(it) }.getOrNull()
        }

        return SearchPage(
            items = topics,
            nextUrl = paging?.next,
            isEnd = paging?.isEnd ?: (paging?.next == null),
        )
    }

    override suspend fun fetchHotSearches(): List<HotSearchItem> {
        val json = environment.fetchJson(ZHIHU_HOT_SEARCH_URL, "") ?: return emptyList()
        val queries = json["hot_search_queries"] as? JsonArray ?: return emptyList()
        return queries.take(15).mapNotNull { element ->
            runCatching { ZhihuJson.decodeJson<HotSearchItem>(element) }.getOrNull()
        }
    }

    override fun isHotSearchEnabled(): Boolean =
        settingsRepository.current.interaction.showSearchHotSearch

    override fun isSearchHistoryEnabled(): Boolean =
        settingsRepository.current.interaction.showSearchHistory

    override suspend fun setTopicFollowing(
        topicId: String,
        following: Boolean,
    ): Result<Unit> = runCatching {
        val endpoint = "https://www.zhihu.com/api/v4/topics/$topicId/followers"
        val response = if (following) environment.postSigned(endpoint) else environment.deleteSigned(endpoint)
        response.raiseForStatus()
    }

    override suspend fun setMemberFollowing(
        urlToken: String,
        following: Boolean,
    ): Result<Unit> = runCatching {
        val endpoint = "https://www.zhihu.com/api/v4/members/$urlToken/followers"
        val response = if (following) environment.postSigned(endpoint) else environment.deleteSigned(endpoint)
        response.raiseForStatus()
    }

    override fun getSearchHistory(): List<String> =
        historyStorage.history

    override fun saveSearchHistory(history: List<String>) {
        historyStorage.saveHistory(history)
    }

    override fun clearSearchHistory() {
        historyStorage.clear()
    }

    private fun buildSearchUrl(
        query: String,
        searchTab: SearchTab,
        sortOption: SearchSortOption,
        contentType: SearchContentType,
        timeRange: SearchTimeRange,
        restrictedMemberHashId: String,
    ): String {
        val hasActiveFilter = sortOption != SearchSortOption.Default ||
            contentType != SearchContentType.All ||
            timeRange != SearchTimeRange.All
        val params = buildList {
            add("gk_version" to "gz-gaokao")
            add(
                "t" to when (searchTab) {
                    SearchTab.People -> "people"
                    SearchTab.Topic -> "topic"
                    SearchTab.General -> "general"
                },
            )
            add("q" to query)
            add("correction" to "1")
            add("offset" to "0")
            add("limit" to "20")
            add("search_source" to if (hasActiveFilter) "Filter" else "Normal")
            add("show_all_topics" to if (searchTab == SearchTab.Topic) "1" else "0")
            if (restrictedMemberHashId.isNotBlank()) {
                add("filter_fields" to "")
                add("lc_idx" to "0")
                add("restricted_scene" to "member")
                add("restricted_field" to "member_hash_id")
                add("restricted_value" to restrictedMemberHashId)
            }
            if (contentType.value.isNotEmpty()) {
                add("vertical" to contentType.value)
                add("vertical_info" to SEARCH_VERTICAL_INFO)
            }
            if (sortOption.value.isNotEmpty()) {
                add("sort" to sortOption.value)
            }
            if (timeRange.value.isNotEmpty()) {
                add("time_interval" to timeRange.value)
            }
        }.joinToString("&") { (key, value) ->
            "$key=${value.encodeURLParameter(spaceToPlus = true)}"
        }
        return "https://www.zhihu.com/api/v4/search_v3?$params"
    }

    private fun decodeTopicSearchResult(element: JsonElement): TopicSearchResult? {
        val entry = element as? JsonObject ?: return null
        val objectJson = entry["object"] as? JsonObject ?: return null
        val decoded = runCatching { ZhihuJson.decodeJson<TopicSearchDto>(objectJson) }.getOrNull() ?: return null
        if (decoded.type != "topic") return null
        return TopicSearchResult(
            topic = SearchTopicItem(
                id = decoded.id,
                name = decoded.name.replace("<em>", "").replace("</em>", ""),
                avatarUrl = decoded.avatarUrl,
                topicType = decoded.topicType,
                url = decoded.url,
            ),
            excerpt = decoded.excerpt.replace("<em>", "").replace("</em>", ""),
            visitCount = decoded.visitCount,
            discussCount = decoded.topAnswerCount,
            isFollowing = decoded.isFollowing,
        )
    }
}
