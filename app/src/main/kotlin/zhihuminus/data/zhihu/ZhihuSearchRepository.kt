package com.zhihuminus.data.zhihu

import com.zhihuminus.core.environment.PaginationEnvironment
import com.zhihuminus.core.settings.AppSettingsRepository
import com.zhihuminus.core.util.Log
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.flattenFeeds
import com.zhihuminus.data.local.SearchHistoryStore
import com.zhihuminus.data.toDisplayItem
import com.zhihuminus.data.zhihu.api.ZhihuSearchApi
import com.zhihuminus.data.zhihu.dto.CommonFeedDto
import com.zhihuminus.data.zhihu.dto.FeedTargetDto
import com.zhihuminus.data.zhihu.dto.MemberItemDto
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class ZhihuSearchRepository(
    private val api: ZhihuSearchApi,
    private val settingsRepository: AppSettingsRepository,
    private val historyStorage: SearchHistoryStore,
) : SearchRepository {
    constructor(
        environment: PaginationEnvironment,
        settingsRepository: AppSettingsRepository,
        historyStorage: SearchHistoryStore,
    ) : this(
        api = ZhihuApiImpl(environment),
        settingsRepository = settingsRepository,
        historyStorage = historyStorage,
    )

    override suspend fun searchGeneral(
        query: String,
        tab: SearchTab,
        sort: SearchSortOption,
        contentType: SearchContentType,
        timeRange: SearchTimeRange,
        restrictedMemberHashId: String,
        nextUrl: String?,
    ): SearchPage<FeedDisplayItem> {
        val response = api.search(
            query = query,
            tab = "general",
            sort = sort.value,
            vertical = contentType.value,
            timeInterval = timeRange.value,
            restrictedMemberHashId = restrictedMemberHashId,
            nextUrl = nextUrl,
        )

        val feeds = response.data.mapNotNull { item ->
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
                Log.e("ZhihuSearchRepository", "Failed to decode search feed target: $obj", e)
                null
            }
        }
        val displayItems = feeds.flattenFeeds().map { it.toDisplayItem() }
        val paging = response.paging

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
        val response = api.search(
            query = query,
            tab = "people",
            sort = sort.value,
            timeInterval = timeRange.value,
            restrictedMemberHashId = restrictedMemberHashId,
            nextUrl = nextUrl,
        )

        val people = response.data.mapNotNull { item ->
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
                Log.e("ZhihuSearchRepository", "Failed to decode member in search: $obj", e)
                null
            }
        }
        val paging = response.paging

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
        val response = api.search(
            query = query,
            tab = "topic",
            sort = sort.value,
            timeInterval = timeRange.value,
            restrictedMemberHashId = restrictedMemberHashId,
            nextUrl = nextUrl,
        )

        val topics = response.data.mapNotNull { element ->
            decodeTopicSearchResult(element.obj).also { result ->
                if (result == null) {
                    Log.e("ZhihuSearchRepository", "Topic search result missing valid topic object: $element")
                }
            }
        }
        val paging = response.paging

        return SearchPage(
            items = topics,
            nextUrl = paging?.next,
            isEnd = paging?.isEnd ?: (paging?.next == null),
        )
    }

    override suspend fun fetchHotSearches(): List<HotSearchItem> =
        api.getHotSearches().map {
            HotSearchItem(
                query = it.query,
                hotShow = it.hotShow,
                label = it.label,
            )
        }

    override fun isHotSearchEnabled(): Boolean =
        settingsRepository.current.interaction.showSearchHotSearch

    override fun isSearchHistoryEnabled(): Boolean =
        settingsRepository.current.interaction.showSearchHistory

    override suspend fun setTopicFollowing(
        topicId: String,
        following: Boolean,
    ): Result<Unit> = runCatching {
        api.followTopic(topicId, following)
    }

    override suspend fun setMemberFollowing(
        urlToken: String,
        following: Boolean,
    ): Result<Unit> = runCatching {
        api.followMember(urlToken, following)
    }

    override fun getSearchHistory(): List<String> =
        historyStorage.history

    override fun saveSearchHistory(history: List<String>) {
        historyStorage.saveHistory(history)
    }

    override fun clearSearchHistory() {
        historyStorage.clear()
    }

    private fun decodeTopicSearchResult(obj: JsonElement?): TopicSearchResult? {
        val objectJson = obj as? JsonObject ?: return null
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
