/*
 * Zhihu-- - Free & Ad-Free Zhihu client for Android.
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

package com.zhihuminus.data

import com.zhihuminus.data.zhihu.dto.FeedDto
import com.zhihuminus.data.zhihu.mapper.navDestination
import com.zhihuminus.data.zhihu.mapper.stableTargetKey
import com.zhihuminus.navigation.NavDestination
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class FeedImageLayout {
    NONE,
    RIGHT_THUMBNAIL,
    BOTTOM_GALLERY,
}

@Serializable
data class FeedDisplayItem(
    val title: String,
    val summary: String?,
    val details: String,
    val feed: FeedDto? = null,
    val navDestinationJson: String? = null,
    val avatarSrc: String? = null,
    val authorName: String? = null,
    val authorBadge: OfficialBadge? = null,
    val content: String? = null,
    val contentTypeLabel: String? = null,
    val publishTimeSeconds: Long? = null,
    val sourceLabel: String? = null,
    val images: List<String> = emptyList(),
    val imageLayout: FeedImageLayout = FeedImageLayout.NONE,
) {
    val stableKey: String
        get() = navDestinationJson
            ?: feed?.target?.stableTargetKey
            ?: "$title|${summary.orEmpty()}|$details"
}

private val feedNavigationJson = Json {
    ignoreUnknownKeys = true
}

val FeedDisplayItem.navDestination: NavDestination?
    get() = navDestinationJson
        ?.let { runCatching { feedNavigationJson.decodeFromString<NavDestination>(it) }.getOrNull() }
        ?: feed?.target?.navDestination

fun NavDestination.toFeedDisplayItemNavDestinationJson(): String =
    feedNavigationJson.encodeToString<NavDestination>(this)
