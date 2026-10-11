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

package com.zhihuminus.account

import com.zhihuminus.data.common.ZhihuJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

const val DEFAULT_ZHIHU_USER_AGENT = ZHIHU_DESKTOP_USER_AGENT

@Serializable
data class ZhihuAccountProfileSnapshot(
    val id: String = "",
    val name: String = "",
    val urlToken: String? = null,
    val userType: String = "",
    val avatarUrl: String? = null,
)

@Serializable
data class ZhihuAccountSession(
    val login: Boolean = false,
    val username: String = "",
    val cookies: MutableMap<String, String> = mutableMapOf(),
    val userAgent: String = DEFAULT_ZHIHU_USER_AGENT,
    val profile: ZhihuAccountProfileSnapshot? = null,
    val self: JsonElement? = null,
    val mobileAccessToken: String? = null,
    val mobileRefreshToken: String? = null,
    val mobileTokenType: String? = null,
    val mobileTokenExpiresAt: Long? = null,
)

interface ZhihuAccountSessionStore {
    fun readText(): String?

    fun writeText(text: String)

    fun delete()
}

class ZhihuAccountRepository(
    private val store: ZhihuAccountSessionStore,
    private val json: Json = ZhihuJson.json,
) {
    fun load(): ZhihuAccountSession = runCatching {
        store
            .readText()
            ?.takeIf { it.isNotBlank() }
            ?.let { json.decodeFromString<ZhihuAccountSession>(it) }
            ?.let { session ->
                if (session.userAgent.isBlank() ||
                    session.userAgent.contains("Chrome/9.1.0.0") ||
                    session.userAgent.contains("Ubuntu/10.10")
                ) {
                    val migrated = session.copy(userAgent = DEFAULT_ZHIHU_USER_AGENT)
                    save(migrated)
                    migrated
                } else {
                    session
                }
            }
            ?: ZhihuAccountSession()
    }.getOrDefault(ZhihuAccountSession())

    fun save(session: ZhihuAccountSession) {
        store.writeText(json.encodeToString(session))
    }

    fun clear() {
        store.delete()
    }
}
