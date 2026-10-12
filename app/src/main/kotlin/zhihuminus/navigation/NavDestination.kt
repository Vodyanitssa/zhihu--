/*
 * Zhihu-- - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 overlay).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zhihuminus.navigation

import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.router.AppRouter
import com.zhihuminus.navigation.router.RouteResolution
import com.zhihuminus.navigation.router.ZhihuWebUrlResolver
import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavDestination

/**
 * 底部栏和主 pager 使用的 tab 目标。
 * 需要进入主壳时应使用 [MainTabs]，再由主壳选择对应 tab。
 */
interface TopLevelDestination {
    val name: String
}

/**
 * 主壳真实使用的导航目的地。
 */
@Serializable
data object MainTabs : NavDestination

/**
 * 主 pager 的顶层 tab 目标。
 */
@Serializable
data object Home : TopLevelDestination {
    override val name: String
        get() = "Home"
}

/**
 * 主 pager 的顶层 tab 目标。
 */
@Serializable
data object Follow : TopLevelDestination {
    override val name: String
        get() = "Follow"
}

/**
 * 历史记录目的地。
 */
@Serializable
data object History : NavDestination

/**
 * 账号与设置目的地。
 */
@Serializable
data object Account : NavDestination {
    val name: String
        get() = "Account"

    @Serializable
    data class AppearanceSettings(
        val setting: String = "",
    ) : NavDestination

    @Serializable
    data object ReadingSettings : NavDestination

    @Serializable
    data class SystemAndUpdateSettings(
        val setting: String = "",
    ) : NavDestination

    @Serializable
    data class NotificationSettings(
        val setting: String = "",
    ) : NavDestination

    @Serializable
    data object SettingsSearch : NavDestination

    @Serializable
    data object OpenSourceLicenses : NavDestination
}

/**
 * 主 pager 的顶层 tab 目标。
 */
@Serializable
data object Daily : TopLevelDestination {
    override val name: String
        get() = "Daily"
}

/**
 * 稍后再看顶层 tab 目标。
 */
@Serializable
data object WatchLater : TopLevelDestination {
    override val name: String
        get() = "WatchLater"
}

@Serializable
data object Notification : NavDestination {
    @Serializable
    data class Entry(
        val entryName: String,
        val title: String,
    ) : NavDestination

    @Serializable
    data object Invitations : NavDestination

    @Serializable
    data class Message(
        val peerId: String,
        val name: String = "",
        val avatarUrl: String = "",
    ) : NavDestination
}

@Serializable
data class Search(
    val query: String = "",
    val restrictedMemberHashId: String = "",
    val restrictedMemberName: String = "",
) : NavDestination {
    val isRestrictedToMember: Boolean
        get() = restrictedMemberHashId.isNotBlank()
}

@Serializable
data class Collections(
    val userToken: String,
) : NavDestination

@Serializable
data class CollectionContent(
    val collectionId: String,
) : NavDestination

@Serializable
data class PostDestination(
    val title: String = "loading...",
    @SerialName("article_type_1")
    val type: PostType,
    val id: Long,
    val authorName: String = "loading...",
    val authorBio: String = "loading...",
    val avatarSrc: String? = null,
    val excerpt: String? = null,
    val readingQueueSourceId: String? = null,
) : NavDestination {
    override fun hashCode(): Int = id.hashCode()

    override fun equals(other: Any?): Boolean = other is PostDestination && other.id == id && other.type == type
}

@Serializable
data class CommentHolder(
    val commentId: String,
    val article: NavDestination,
) : NavDestination

@Serializable
data class Question(
    val questionId: Long,
    val title: String = "loading...",
    val readingQueueSourceId: String? = null,
) : NavDestination {
    override fun hashCode(): Int = questionId.hashCode()

    override fun equals(other: Any?): Boolean = other is Question && other.questionId == questionId
}

@Serializable
data class Person(
    /**
     * 32 位十六进制字符。
     */
    val id: String,
    /**
     * 用在 URL 中的可读 token。
     */
    val urlToken: String,
    val name: String = "loading...",
    val jumpTo: String = "",
) : NavDestination {
    override fun hashCode(): Int {
        if (id != EMPTY_ID) {
            return id.hashCode()
        }
        return urlToken.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is Person) {
            if (id != EMPTY_ID && other.id != EMPTY_ID) {
                return other.id == id
            }
            return other.urlToken == urlToken
        }
        return false
    }

    val userTokenOrId get() = urlToken.takeIf { it.isNotEmpty() } ?: id

    companion object {
        const val EMPTY_ID = "00000000000000000000000000000000"
    }
}

@Serializable
data class Video(
    val id: Long,
) : NavDestination

@Serializable
data class Topic(
    val id: String,
    val name: String = "",
    val section: String = "",
) : NavDestination

@Serializable
data class Column(
    val columnId: String,
) : NavDestination

fun NavDestination.withReadingQueueSource(sourceId: String?): NavDestination = when (this) {
    is PostDestination -> copy(readingQueueSourceId = sourceId)
    is Question -> copy(readingQueueSourceId = sourceId)
    else -> this
}

/**
 * 链接解析门面，兼容旧调用点。内部委托给统一的 [AppRouter.resolveDestination]。
 */
fun resolveContent(url: String): NavDestination? = AppRouter.resolveDestination(url)

/**
 * [Url] 链接解析门面，兼容旧调用点。
 */
fun resolveContent(url: Url): NavDestination? =
    ZhihuWebUrlResolver.resolve(url) ?: runCatching { AppRouter.resolve(url) }.getOrNull()?.let { resolution ->
        when (resolution) {
            is RouteResolution.Screen -> resolution.destination
            is RouteResolution.Tab -> MainTabs
        }
    }
