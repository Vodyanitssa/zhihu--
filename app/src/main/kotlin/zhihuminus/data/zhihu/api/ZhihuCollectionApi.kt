package com.zhihuminus.data.zhihu.api

import com.zhihuminus.data.zhihu.dto.CollectionDto
import com.zhihuminus.data.zhihu.dto.CollectionItemsPageDto
import com.zhihuminus.data.zhihu.dto.CollectionResponseDto

interface ZhihuCollectionApi {
    /**
     * 获取收藏夹列表
     * @param type 内容类型: "answer" 或 "article"
     * @param id 内容 ID
     * @return 收藏夹列表响应
     */
    suspend fun getCollections(type: String, id: Long): CollectionResponseDto

    /**
     * 收藏内容到指定收藏夹
     * @param type 内容类型: "answer" 或 "article"
     * @param id 内容 ID
     * @param collectionId 收藏夹 ID
     */
    suspend fun addToCollection(type: String, id: Long, collectionId: String)

    /**
     * 取消收藏
     * @param type 内容类型: "answer" 或 "article"
     * @param id 内容 ID
     * @param collectionId 收藏夹 ID
     */
    suspend fun removeFromCollection(type: String, id: Long, collectionId: String)

    /**
     * 创建新收藏夹
     * @param title 收藏夹标题
     * @param description 描述
     * @param isPublic 是否公开
     * @return 新创建的收藏夹
     */
    suspend fun createCollection(title: String, description: String, isPublic: Boolean): CollectionDto

    /**
     * 获取用户收藏夹列表
     * @param urlToken 用户 urlToken
     * @param nextUrl 分页续页 URL
     */
    suspend fun getUserCollections(urlToken: String, nextUrl: String? = null): CollectionResponseDto

    /**
     * 删除收藏夹
     * @param collectionId 收藏夹 ID
     */
    suspend fun deleteCollection(collectionId: String): Boolean

    /**
     * 获取指定收藏夹详情
     * @param collectionId 收藏夹 ID
     */
    suspend fun getCollection(collectionId: String): CollectionDto

    /**
     * 获取收藏夹内容条目
     * @param collectionId 收藏夹 ID
     * @param offset 偏移量
     * @param limit 每页数量
     * @param nextUrl 分页续页 URL
     */
    suspend fun getCollectionItems(
        collectionId: String,
        offset: Int? = null,
        limit: Int = 20,
        nextUrl: String? = null,
    ): CollectionItemsPageDto
}
