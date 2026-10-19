package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.core.util.Log
import com.zhihuminus.data.cache.PostContentCache
import com.zhihuminus.data.zhihu.api.ZhihuCollectionApi
import com.zhihuminus.data.zhihu.api.ZhihuHistoryApi
import com.zhihuminus.data.zhihu.api.ZhihuPeopleApi
import com.zhihuminus.data.zhihu.api.ZhihuPostApi
import com.zhihuminus.data.zhihu.mapper.toDomain
import com.zhihuminus.data.zhihu.mapper.toPost
import com.zhihuminus.feature.collection.Collection
import com.zhihuminus.feature.post.Post
import com.zhihuminus.feature.post.PostRepository
import com.zhihuminus.feature.post.PostType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class ZhihuPostRepository(
    private val postApi: ZhihuPostApi,
    private val collectionApi: ZhihuCollectionApi,
    private val peopleApi: ZhihuPeopleApi,
    private val historyApi: ZhihuHistoryApi,
    private val postCache: PostContentCache = PostContentCache,
) : PostRepository {
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun refreshCache(type: PostType, id: Long) {
        cacheScope.launch {
            try {
                getPost(type, id)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("ZhihuPostRepository", "Failed to refresh cache $type/$id", e)
            }
        }
    }

    override suspend fun getPost(type: PostType, id: Long): Post {
        val post = when (type) {
            PostType.Answer -> postApi.getAnswer(id).toPost()
            PostType.Article -> postApi.getArticle(id).toPost()
            PostType.Pin -> postApi.getPin(id).toPost()
        }
        postCache.put(post)
        return post
    }

    override suspend fun getCachedPost(type: PostType, id: Long): Post? =
        postCache.get(type, id)

    override suspend fun vote(postType: PostType, id: Long, vote: String): Int {
        val result = when (postType) {
            PostType.Answer -> postApi.voteAnswer(id, vote)
            PostType.Article -> postApi.voteArticle(id, vote)
            PostType.Pin -> when (vote) {
                "up" -> postApi.likePin(id)
                "neutral" -> postApi.unlikePin(id)
                else -> throw UnsupportedOperationException("Pin does not support vote: $vote")
            }
        }
        refreshCache(postType, id)
        return result
    }

    override suspend fun submitPinPollVote(pollId: String, optionId: String) = postApi.submitPinPollVote(pollId, optionId)

    override suspend fun getCollections(postType: PostType, id: Long): List<Collection> {
        val type = when (postType) {
            PostType.Answer -> "answer"
            PostType.Article -> "article"
            PostType.Pin -> "pin"
        }
        val response = collectionApi.getCollections(type, id)
        return response.data.map { it.toDomain() }
    }

    override suspend fun addToCollection(postType: PostType, id: Long, collectionId: String) {
        val type = when (postType) {
            PostType.Answer -> "answer"
            PostType.Article -> "article"
            PostType.Pin -> "pin"
        }
        collectionApi.addToCollection(type, id, collectionId)
        refreshCache(postType, id)
    }

    override suspend fun removeFromCollection(postType: PostType, id: Long, collectionId: String) {
        val type = when (postType) {
            PostType.Answer -> "answer"
            PostType.Article -> "article"
            PostType.Pin -> "pin"
        }
        collectionApi.removeFromCollection(type, id, collectionId)
        refreshCache(postType, id)
    }

    override suspend fun createCollection(title: String, description: String, isPublic: Boolean): Collection =
        collectionApi.createCollection(title, description, isPublic).toDomain()

    override suspend fun followMember(urlToken: String, follow: Boolean) {
        if (follow) peopleApi.followMember(urlToken) else peopleApi.unfollowMember(urlToken)
        postCache.updateAuthorFollowing(urlToken, follow)
    }

    override suspend fun putCachedPost(post: Post) {
        postCache.put(post)
    }

    override suspend fun recordHistory(postType: PostType, id: Long) {
        val apiType = when (postType) {
            PostType.Answer -> "answer"
            PostType.Article -> "article"
            PostType.Pin -> "pin"
        }
        val contentToken = id.toString()
        historyApi.addHistory(contentToken, apiType)
        historyApi.markAsRead(contentToken, apiType)
    }
}
