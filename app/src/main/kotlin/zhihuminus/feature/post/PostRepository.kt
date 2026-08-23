package com.zhihuminus.feature.post

import com.zhihuminus.data.Collection

interface PostRepository {
    suspend fun getPost(type: PostType, id: Long): Post

    /**
     * 投票（Answer/Article 支持 up/down/neutral，Pin 仅支持 up/neutral）
     * @param postType 内容类型
     * @param id 内容 ID
     * @param vote 投票类型: "up", "down", "neutral"
     * @return 投票后的赞同数
     */
    suspend fun vote(postType: PostType, id: Long, vote: String): Int

    /**
     * Pin 投票
     * @param pollId 投票 ID
     * @param optionId 选项 ID
     */
    suspend fun submitPinPollVote(pollId: String, optionId: String)

    /**
     * 获取收藏夹列表
     * @param postType 内容类型
     * @param id 内容 ID
     * @return 收藏夹列表
     */
    suspend fun getCollections(postType: PostType, id: Long): List<Collection>

    /**
     * 收藏内容到指定收藏夹
     * @param postType 内容类型
     * @param id 内容 ID
     * @param collectionId 收藏夹 ID
     */
    suspend fun addToCollection(postType: PostType, id: Long, collectionId: String)

    /**
     * 取消收藏
     * @param postType 内容类型
     * @param id 内容 ID
     * @param collectionId 收藏夹 ID
     */
    suspend fun removeFromCollection(postType: PostType, id: Long, collectionId: String)

    /**
     * 创建新收藏夹
     * @param title 收藏夹标题
     * @param description 描述
     * @param isPublic 是否公开
     */
    suspend fun createCollection(title: String, description: String, isPublic: Boolean): Collection

    /**
     * 关注/取消关注用户
     * @param urlToken 用户 urlToken
     * @param follow true 为关注，false 为取消关注
     */
    suspend fun followMember(urlToken: String, follow: Boolean)

    /**
     * 记录阅读历史并标记已读
     * @param postType 内容类型
     * @param id 内容 ID
     */
    suspend fun recordHistory(postType: PostType, id: Long)
}
