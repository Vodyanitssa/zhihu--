package com.zhihuminus.data.zhihu.api

import com.zhihuminus.core.environment.ZhihuApiEnvironment
import com.zhihuminus.core.environment.putSigned
import com.zhihuminus.core.util.raiseForStatus
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.dto.ColumnArticlePage
import com.zhihuminus.data.zhihu.dto.ColumnDetailDto

open class ZhihuColumnApi(
    protected val environment: ZhihuApiEnvironment,
) {
    /**
     * 获取专栏文章列表（分页）
     */
    open suspend fun getColumnArticles(columnId: String, nextUrl: String? = null): ColumnArticlePage {
        val url = nextUrl
            ?: "https://www.zhihu.com/api/v4/columns/$columnId/items?limit=10&offset=0&ws_qiangzhisafe=0"
        val json = environment.fetchJson(url, "")
            ?: throw IllegalStateException("Failed to fetch column articles for $columnId")
        return ZhihuJson.decodeJson(json)
    }

    /**
     * 获取专栏详情
     */
    open suspend fun getColumnDetail(columnId: String): ColumnDetailDto {
        val url = "https://www.zhihu.com/api/v4/columns/$columnId"
        val json = environment.fetchJson(url, "")
            ?: throw IllegalStateException("Failed to fetch column detail for $columnId")
        return ZhihuJson.decodeJson(json)
    }

    /**
     * 关注/取消关注专栏（知乎此接口用 PUT，与话题的 POST/DELETE 不同）
     */
    open suspend fun setFollowing(columnId: String, following: Boolean) {
        val action = if (following) "follow" else "unfollow"
        environment.putSigned("https://www.zhihu.com/api/v4/columns/$columnId/$action").raiseForStatus()
    }
}
