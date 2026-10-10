package com.zhihuminus.data.local

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File

interface SearchHistoryStore {
    val history: List<String>

    fun add(query: String)

    fun saveHistory(history: List<String>)

    fun clear()
}

class FileSearchHistoryStore(
    private val file: File,
    private val maxSize: Int = 20,
) : SearchHistoryStore {
    constructor(context: Context, maxSize: Int = 20) : this(
        file = File(context.applicationContext.filesDir, "search_history.json"),
        maxSize = maxSize,
    )

    private val _history = mutableListOf<String>()
    override val history: List<String> get() = synchronized(this) { _history.toList() }

    init {
        load()
    }

    override fun add(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        synchronized(this) {
            _history.remove(trimmed)
            _history.add(0, trimmed)
            while (_history.size > maxSize) {
                _history.removeAt(_history.lastIndex)
            }
            save()
        }
    }

    override fun saveHistory(history: List<String>) {
        synchronized(this) {
            _history.clear()
            _history.addAll(history.take(maxSize))
            save()
        }
    }

    override fun clear() {
        synchronized(this) {
            _history.clear()
            save()
        }
    }

    private fun load() {
        if (!file.exists() || !file.isFile) return
        try {
            val content = file.readText()
            val list = Json.decodeFromString<List<String>>(content)
            _history.clear()
            _history.addAll(list.take(maxSize))
        } catch (_: Exception) {
            _history.clear()
        }
    }

    private fun save() {
        try {
            file.parentFile?.mkdirs()
            val content = Json.encodeToString(_history)
            val tempFile = File(file.parentFile, "${file.name}.tmp")
            tempFile.writeText(content)
            if (!tempFile.renameTo(file)) {
                file.delete()
                if (!tempFile.renameTo(file)) {
                    tempFile.copyTo(file, overwrite = true)
                    tempFile.delete()
                }
            }
        } catch (_: Exception) {
            // 写入失败时保持内存中数据可用
        }
    }
}

fun SearchHistoryStore(context: Context, maxSize: Int = 20): SearchHistoryStore =
    FileSearchHistoryStore(context, maxSize)
