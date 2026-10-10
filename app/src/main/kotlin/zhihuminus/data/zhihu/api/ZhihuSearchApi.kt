package com.zhihuminus.data.zhihu.api

import com.zhihuminus.core.environment.ZhihuApiEnvironment
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.dto.HotSearchItemDto
import com.zhihuminus.data.zhihu.dto.PagingDto
import com.zhihuminus.data.zhihu.dto.SearchItemDto
import com.zhihuminus.data.zhihu.dto.SearchResponseDto
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

private const val SEARCH_INCLUDE = "data[*].highlight,object,type"
private const val SEARCH_VERTICAL_INFO = "0,0,0,0,0,0,0,0,0,0,0,0"
private const val ZHIHU_HOT_SEARCH_URL = "https://www.zhihu.com/api/v4/search/hot_search_queries"

open class ZhihuSearchApi(
    protected val environment: ZhihuApiEnvironment,
) {
    /**
     * 搜索知乎内容/用户/话题
     *
     * @param query 搜索关键词
     * @param tab 搜索 Tab: general, people, topic
     * @param sort 排序方式
     * @param vertical 内容类型
     * @param timeInterval 时间范围
     * @param restrictedMemberHashId 限定用户 hash ID
     * @param nextUrl 分页续页 URL
     */
    open suspend fun search(
        query: String,
        tab: String,
        sort: String = "",
        vertical: String = "",
        timeInterval: String = "",
        restrictedMemberHashId: String = "",
        nextUrl: String? = null,
    ): SearchResponseDto {
        val url = nextUrl ?: buildSearchUrl(
            query = query,
            tab = tab,
            sort = sort,
            vertical = vertical,
            timeInterval = timeInterval,
            restrictedMemberHashId = restrictedMemberHashId,
        )
        return fetchSearchPage(url)
    }

    /**
     * 按完整 URL 加载下一页搜索结果
     */
    open suspend fun fetchSearchPage(url: String): SearchResponseDto {
        val json = environment.fetchJson(url, SEARCH_INCLUDE)
            ?: throw IllegalStateException("搜索响应为空")
        val jsonArray = json["data"] as? JsonArray ?: JsonArray(emptyList())
        val items = jsonArray.mapNotNull { element ->
            try {
                ZhihuJson.decodeJson<SearchItemDto>(element)
            } catch (e: Exception) {
                environment.logDecodeFailure("ZhihuSearchApi", element, e)
                null
            }
        }
        val paging = (json["paging"] as? JsonObject)?.let {
            runCatching { ZhihuJson.decodeJson<PagingDto>(it) }.getOrNull()
        }
        return SearchResponseDto(data = items, paging = paging)
    }

    /**
     * 获取热搜词条列表
     */
    open suspend fun getHotSearches(): List<HotSearchItemDto> {
        val json = environment.fetchJson(ZHIHU_HOT_SEARCH_URL, "") ?: return emptyList()
        val queries = json["hot_search_queries"] as? JsonArray ?: return emptyList()
        return queries.take(15).mapNotNull { element ->
            runCatching { ZhihuJson.decodeJson<HotSearchItemDto>(element) }.getOrNull()
        }
    }

    private fun buildSearchUrl(
        query: String,
        tab: String,
        sort: String,
        vertical: String,
        timeInterval: String,
        restrictedMemberHashId: String,
    ): String {
        val hasActiveFilter = sort.isNotEmpty() || vertical.isNotEmpty() || timeInterval.isNotEmpty()
        val params = buildList {
            add("gk_version" to "gz-gaokao")
            add("t" to tab)
            add("q" to query)
            add("correction" to "1")
            add("offset" to "0")
            add("limit" to "20")
            add("search_source" to if (hasActiveFilter) "Filter" else "Normal")
            add("show_all_topics" to if (tab == "topic") "1" else "0")
            if (restrictedMemberHashId.isNotBlank()) {
                add("filter_fields" to "")
                add("lc_idx" to "0")
                add("restricted_scene" to "member")
                add("restricted_field" to "member_hash_id")
                add("restricted_value" to restrictedMemberHashId)
            }
            if (vertical.isNotEmpty()) {
                add("vertical" to vertical)
                add("vertical_info" to SEARCH_VERTICAL_INFO)
            }
            if (sort.isNotEmpty()) {
                add("sort" to sort)
            }
            if (timeInterval.isNotEmpty()) {
                add("time_interval" to timeInterval)
            }
        }.joinToString("&") { (key, value) ->
            "$key=${value.encodeURLParameter(spaceToPlus = true)}"
        }
        return "https://www.zhihu.com/api/v4/search_v3?$params"
    }
}
