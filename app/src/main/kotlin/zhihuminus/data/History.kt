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

package com.zhihuminus.data

data class HistoryDeletePair(
    val contentToken: String,
    val contentType: String,
)

/**
 * 在线浏览历史记录的业务模型。
 *
 * 由 [com.zhihuminus.data.zhihu.ZhihuHistoryRepository] 从 API DTO 解析而来，
 * ViewModel 层只依赖此类型，不感知 API 细节。
 */
data class HistoryItem(
    val title: String,
    val summary: String,
    val details: String,
    val authorName: String?,
    val contentTypeLabel: String,
    val actionUrl: String,
    val contentToken: String,
    val contentType: String,
)
