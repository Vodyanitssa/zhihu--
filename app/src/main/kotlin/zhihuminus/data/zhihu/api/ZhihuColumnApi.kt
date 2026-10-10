package com.zhihuminus.data.zhihu.api

import com.zhihuminus.core.environment.ZhihuApiEnvironment
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.dto.ColumnArticlePage

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
}
