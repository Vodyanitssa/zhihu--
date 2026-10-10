package com.zhihuminus.data.zhihu.api

import com.zhihuminus.data.zhihu.dto.ColumnArticlePage

interface ZhihuColumnApi {
    /**
     * 获取专栏文章列表
     * @param columnId 专栏 ID
     * @param nextUrl 分页续页 URL（为 null 时从第一页开始）
     */
    suspend fun getColumnArticles(columnId: String, nextUrl: String?): ColumnArticlePage
}
