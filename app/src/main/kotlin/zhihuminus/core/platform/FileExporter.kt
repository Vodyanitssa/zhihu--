package com.zhihuminus.core.platform

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

/**
 * 将 Bitmap 保存到设备相册的工具类。
 *
 * 封装了 Android MediaStore API，兼容 Android Q (API 29) 和以上的版本。
 * 图片保存在 `Pictures/Zhihu--/` 目录下。
 */
class FileExporter(
    private val context: Context,
) {
    /**
     * 将 Bitmap 保存到系统相册。
     *
     * @param displayName 文件名（含扩展名，如 `zhihu--_xxx.png`）
     * @param bitmap 要保存的 Bitmap
     */
    suspend fun saveToGallery(
        displayName: String,
        bitmap: Bitmap,
    ): Unit = withContext(Dispatchers.IO) {
        saveImageToMediaStore(
            displayName = displayName,
            mimeType = "image/png",
            relativePath = Environment.DIRECTORY_PICTURES + "/Zhihu--",
        ) { outputStream ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 80, outputStream)) {
                throw IllegalStateException("Failed to encode image")
            }
        }
    }

    private fun saveImageToMediaStore(
        displayName: String,
        mimeType: String,
        relativePath: String,
        writeImage: (OutputStream) -> Unit,
    ) {
        val contentValues = ContentValues().apply {
            put(MediaColumns.DISPLAY_NAME, displayName)
            put(MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val imageUri = resolver.insert(collection, contentValues)
            ?: throw IllegalStateException("Failed to create MediaStore entry")

        try {
            resolver.openOutputStream(imageUri)?.use(writeImage)
                ?: throw IllegalStateException("Failed to open MediaStore output stream")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }
        } catch (e: Exception) {
            resolver.delete(imageUri, null, null)
            throw e
        }
    }
}
