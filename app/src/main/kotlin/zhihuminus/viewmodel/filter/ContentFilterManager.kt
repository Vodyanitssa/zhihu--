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

package com.zhihuminus.viewmodel.filter

import com.zhihuminus.filter.cleanupOldData

/**
 * 内容曝光记录管理器。
 * 只负责维护“某个内容身份在 feed 中被看过几次、是否发生过交互”这类本地状态，
 * 真正的 feed 过滤编排由上层过滤流水线完成。
 */
class ContentFilterManager(
    private val dao: ContentFilterDao,
) {
    /** 记录某个内容身份在 feed 内发生过交互。 */
    suspend fun recordContentInteraction(targetType: String, targetId: String) {
        val recordId = ContentViewRecord.generateId(targetType, targetId)
        dao.markAsInteracted(recordId)
    }

    /** 清理过期曝光记录。 */
    suspend fun cleanupOldData() {
        dao.cleanupOldData()
    }
}
