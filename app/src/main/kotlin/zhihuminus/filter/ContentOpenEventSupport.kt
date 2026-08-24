/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zhihuminus.filter

import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.CollectionContent
import com.zhihuminus.navigation.History
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Notification
import com.zhihuminus.navigation.PostDestination
import com.zhihuminus.navigation.Question
import com.zhihuminus.viewmodel.filter.ContentFilterDatabase
import com.zhihuminus.viewmodel.filter.ContentOpenEvent
import com.zhihuminus.viewmodel.filter.ContentType

data class TrackedContentIdentity(
    val type: String,
    val id: String,
)

data class QuestionAnswerCandidatePartition(
    val previousCandidates: List<PostDestination>,
    val nextCandidates: List<PostDestination>,
)

object ContentOpenFrom {
    const val ANSWER_SWITCH = "answer_switch"
    const val COLLECTION = "collection"
    const val HISTORY = "history"
    const val HOME_FEED = "home_feed"
    const val NOTIFICATION = "notification"
    const val QUESTION_FEED = "question_feed"
    const val UNKNOWN = "unknown"
}

object ContentOpenEventSupport {
    fun toTrackedContentIdentity(destination: NavDestination): TrackedContentIdentity? = when (destination) {
        is PostDestination -> {
            val type = when (destination.type) {
                PostType.Answer -> ContentType.ANSWER
                PostType.Article -> ContentType.ARTICLE
                PostType.Pin -> ContentType.PIN
            }
            TrackedContentIdentity(type = type, id = destination.id.toString())
        }

        is Question -> TrackedContentIdentity(type = ContentType.QUESTION, id = destination.questionId.toString())
        else -> null
    }

    fun inferOpenFrom(
        source: NavDestination?,
        target: NavDestination,
    ): String = when {
        source is PostDestination &&
            source.type == PostType.Answer &&
            target is PostDestination &&
            target.type == PostType.Answer -> ContentOpenFrom.ANSWER_SWITCH

        source is Question -> ContentOpenFrom.QUESTION_FEED
        source is CollectionContent -> ContentOpenFrom.COLLECTION
        source is History -> ContentOpenFrom.HISTORY
        source is Notification -> ContentOpenFrom.NOTIFICATION
        else -> ContentOpenFrom.UNKNOWN
    }

    suspend fun recordOpenEvent(
        database: ContentFilterDatabase,
        destination: NavDestination,
        questionId: Long? = null,
        openFrom: String = ContentOpenFrom.UNKNOWN,
    ) {
        val identity = toTrackedContentIdentity(destination) ?: return
        database
            .contentOpenEventDao()
            .insert(
                ContentOpenEvent(
                    contentType = identity.type,
                    contentId = identity.id,
                    questionId = questionId,
                    openFrom = openFrom,
                ),
            )
    }

    suspend fun getAlreadyOpenedContentIds(
        database: ContentFilterDatabase,
        content: List<Pair<String, String>>,
    ): Set<String> = run {
        val idsToCheck = content.map { (targetType, targetId) ->
            "$targetType:$targetId"
        }
        database
            .contentOpenEventDao()
            .getOpenedContentKeysByKeys(idsToCheck)
            .toSet()
    }

    fun partitionQuestionAnswerCandidates(
        candidates: List<PostDestination>,
        openedAnswerIds: Set<Long>,
        currentArticleId: Long,
        historyIds: Set<Long> = emptySet(),
        previousIds: Set<Long> = emptySet(),
        nextIds: Set<Long> = emptySet(),
    ): QuestionAnswerCandidatePartition {
        val previousCandidates = mutableListOf<PostDestination>()
        val nextCandidates = mutableListOf<PostDestination>()

        candidates.forEach { article ->
            if (article.type != PostType.Answer || article.id == currentArticleId || article.id in historyIds) {
                return@forEach
            }
            if (article.id in previousIds || article.id in nextIds) {
                return@forEach
            }
            if (article.id in openedAnswerIds) {
                previousCandidates.add(article)
            } else {
                nextCandidates.add(article)
            }
        }

        return QuestionAnswerCandidatePartition(
            previousCandidates = previousCandidates,
            nextCandidates = nextCandidates,
        )
    }
}
