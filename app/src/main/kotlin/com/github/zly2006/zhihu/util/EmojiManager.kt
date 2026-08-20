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

package com.github.zly2006.zhihu.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object EmojiManager {
    private const val TAG = "EmojiManager"
    private const val EMOJI_MAPPING_FILE = "emojis/emoji_mapping.json"

    // emoji占位符到文件名的映射，例如 "[感谢]" -> "ganxie.png"
    private val emojiMapping = mutableMapOf<String, String>()

    // emoji文件名到assets路径的映射，例如 "ganxie.png" -> "emojis/images/ganxie.png"
    private val emojiAssetPath = mutableMapOf<String, String>()

    private val mutablePlaceholders = MutableStateFlow<List<String>>(emptyList())

    /** 当前已经缓存、可以用于输入和展示的表情占位符。 */
    val placeholders = mutablePlaceholders.asStateFlow()

    private var isInitialized = false

    /**
     * 初始化emoji管理器，从本地assets加载emoji数据
     */
    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext

        try {
            loadEmojiMapping(context)
            Log.i(TAG, "Emoji manager initialized with ${emojiMapping.size} emojis")
            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize emoji manager", e)
        }
    }

    /**
     * 从本地assets加载emoji映射
     */
    private fun loadEmojiMapping(context: Context) {
        val json = Json { ignoreUnknownKeys = true }
        val mappingJson = context.assets
            .open(EMOJI_MAPPING_FILE)
            .bufferedReader()
            .use { it.readText() }
        val mapping = json.decodeFromString<Map<String, String>>(mappingJson)

        emojiMapping.clear()
        emojiMapping.putAll(mapping)

        emojiAssetPath.clear()
        mapping.values.forEach { fileName ->
            emojiAssetPath[fileName] = "emojis/images/$fileName"
        }
        mutablePlaceholders.value = mapping.keys.toList()
    }

    /**
     * 根据emoji占位符获取本地assets路径
     * @param placeholder emoji占位符，例如 "[感谢]"
     * @return assets路径，例如 "emojis/images/emoji_1.png"，如果emoji不存在返回null
     */
    fun getEmojiPath(placeholder: String): String? {
        val fileName = emojiMapping[placeholder] ?: return null
        return emojiAssetPath[fileName]
    }
}
