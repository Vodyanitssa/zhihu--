package com.zhihuminus.data.local

import android.content.Context
import com.zhihuminus.feature.watchlater.WatchLaterItem
import com.zhihuminus.feature.watchlater.WatchLaterRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class FileWatchLaterRepository(
    private val file: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
) : WatchLaterRepository {
    private val mutex = Mutex()
    private val _itemsFlow = MutableStateFlow<List<WatchLaterItem>>(emptyList())

    override val itemsFlow: StateFlow<List<WatchLaterItem>> = _itemsFlow.asStateFlow()

    init {
        _itemsFlow.value = readItemsFromFile()
    }

    override suspend fun getItems(): List<WatchLaterItem> = mutex.withLock {
        _itemsFlow.value
    }

    override suspend fun addItem(item: WatchLaterItem) {
        mutex.withLock {
            val current = _itemsFlow.value.toMutableList()
            current.removeAll { it.id == item.id && it.type.equals(item.type, ignoreCase = true) }
            current.add(0, item)
            withContext(ioDispatcher) {
                writeItemsToFile(current)
            }
            _itemsFlow.value = current
        }
    }

    override suspend fun deleteItem(id: Long, type: String) {
        mutex.withLock {
            val current = _itemsFlow.value.toMutableList()
            val removed = current.removeAll { it.id == id && it.type.equals(type, ignoreCase = true) }
            if (removed) {
                withContext(ioDispatcher) {
                    writeItemsToFile(current)
                }
                _itemsFlow.value = current
            }
        }
    }

    override suspend fun clearAll() {
        mutex.withLock {
            withContext(ioDispatcher) {
                writeItemsToFile(emptyList())
            }
            _itemsFlow.value = emptyList()
        }
    }

    private fun readItemsFromFile(): List<WatchLaterItem> = try {
        if (file.exists() && file.isFile) {
            val content = file.readText()
            json.decodeFromString<List<WatchLaterItem>>(content)
        } else {
            emptyList()
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun writeItemsToFile(items: List<WatchLaterItem>) {
        try {
            file.parentFile?.mkdirs()
            val text = json.encodeToString(items)
            val tempFile = File(file.parentFile, "${file.name}.tmp")
            tempFile.writeText(text)
            if (!tempFile.renameTo(file)) {
                file.delete()
                if (!tempFile.renameTo(file)) {
                    tempFile.copyTo(file, overwrite = true)
                    tempFile.delete()
                }
            }
        } catch (_: Exception) {
            // 写入失败时内存数据仍有效
        }
    }
}

object LocalWatchLaterRepository {
    @Volatile
    private var instance: WatchLaterRepository? = null

    fun getInstance(context: Context): WatchLaterRepository = instance ?: synchronized(this) {
        instance ?: FileWatchLaterRepository(
            file = File(context.applicationContext.filesDir, "watch_later.json"),
        ).also { instance = it }
    }

    internal fun setInstanceForTesting(repository: WatchLaterRepository?) {
        instance = repository
    }
}
