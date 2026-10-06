package com.zhihuminus.core.settings

import android.content.Context
import com.zhihuminus.core.settings.model.AppSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class JsonAppSettingsRepository(
    private val file: File,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
) : AppSettingsRepository {
    private val mutex = Mutex()
    private val _settingsFlow: MutableStateFlow<AppSettings>

    init {
        val initial = readSettingsFromFile()
        _settingsFlow = MutableStateFlow(initial)
    }

    override val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    override val current: AppSettings get() = _settingsFlow.value

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        mutex.withLock {
            val currentSettings = _settingsFlow.value
            val newSettings = transform(currentSettings)
            if (newSettings != currentSettings) {
                withContext(ioDispatcher) {
                    writeSettingsToFile(newSettings)
                }
                _settingsFlow.value = newSettings
            }
        }
    }

    override fun updateAsync(transform: (AppSettings) -> AppSettings) {
        scope.launch {
            update(transform)
        }
    }

    private fun readSettingsFromFile(): AppSettings = try {
        if (file.exists() && file.isFile) {
            val content = file.readText()
            json.decodeFromString<AppSettings>(content)
        } else {
            AppSettings()
        }
    } catch (_: Exception) {
        AppSettings()
    }

    private fun writeSettingsToFile(settings: AppSettings) {
        try {
            file.parentFile?.mkdirs()
            val text = json.encodeToString(AppSettings.serializer(), settings)
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
            // 写入失败时保持内存中数据最新
        }
    }
}

object AndroidAppSettingsRepository {
    @Volatile
    private var instance: AppSettingsRepository? = null

    fun getInstance(context: Context): AppSettingsRepository = instance ?: synchronized(this) {
        instance ?: JsonAppSettingsRepository(
            file = File(context.applicationContext.filesDir, "settings.json"),
        ).also { instance = it }
    }

    /**
     * 仅供测试环境重置单例使用
     */
    internal fun setInstanceForTesting(repository: AppSettingsRepository?) {
        instance = repository
    }
}
