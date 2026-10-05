package com.zhihuminus.core.platform

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.zhihuminus.core.util.friendlyErrorMessage
import com.zhihuminus.platform.androidUserMessageSink
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
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
            context = context,
            displayName = displayName,
            mimeType = "image/png",
            relativePath = Environment.DIRECTORY_PICTURES + "/Zhihu--",
        ) { outputStream ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 80, outputStream)) {
                throw IllegalStateException("Failed to encode image")
            }
        }
    }
}

internal fun saveImageToMediaStore(
    context: Context,
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

/**
 * 保存图片到相册
 */
suspend fun saveImageToGallery(
    context: Context,
    httpClient: HttpClient,
    imageUrl: String,
) {
    val userMessages = androidUserMessageSink(context)
    try {
        val response = httpClient.get(imageUrl)
        val bytes = response.readRawBytes()
        val fileName = imageUrl.toUri().lastPathSegment ?: "downloaded_image.jpg"
        saveDownloadedImageToGallery(
            context = context,
            imageUrl = imageUrl,
            contentTypeHeader = response.headers[HttpHeaders.ContentType],
            displayName = fileName,
            bytes = bytes,
        )
        userMessages.showShortMessage("图片已保存到相册")
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        userMessages.showShortMessage("保存失败: ${friendlyErrorMessage(e)}")
    }
}

private fun saveDownloadedImageToGallery(
    context: Context,
    imageUrl: String,
    contentTypeHeader: String?,
    displayName: String,
    bytes: ByteArray,
) {
    saveImageToMediaStore(
        context = context,
        displayName = displayName,
        mimeType = resolveArticleExportImageMimeType(
            contentTypeHeader = contentTypeHeader,
            imageUrl = imageUrl,
            imageBytes = bytes,
        ),
        relativePath = Environment.DIRECTORY_PICTURES,
    ) { outputStream ->
        outputStream.write(bytes)
    }
}

/**
 * 分享图片
 * 图片临时保存到 externalCacheDir/share_images/，应用启动时自动清空
 */
suspend fun shareImage(
    context: Context,
    httpClient: HttpClient,
    imageUrl: String,
) {
    val userMessages = androidUserMessageSink(context)
    try {
        val response = httpClient.get(imageUrl)
        val bytes = response.readRawBytes()
        val shareDir = java.io.File(context.externalCacheDir, "share_images").apply { mkdirs() }
        val file = java.io.File(shareDir, "share_${System.currentTimeMillis()}.jpg")
        file.writeBytes(bytes)
        val imageUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, imageUri)
            type = "image/jpeg"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "分享图片"))
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        userMessages.showShortMessage("分享失败: ${friendlyErrorMessage(e)}")
    }
}

/**
 * 清空分享图片缓存目录
 */
fun clearShareImageCache(context: Context) {
    java.io.File(context.externalCacheDir, "share_images").deleteRecursively()
}

private fun resolveArticleExportImageMimeType(
    contentTypeHeader: String?,
    imageUrl: String,
    imageBytes: ByteArray,
): String {
    contentTypeHeader
        ?.substringBefore(';')
        ?.trim()
        ?.takeIf { it.startsWith("image/") }
        ?.let { return it }

    guessImageMimeTypeFromName(imageUrl)?.let { return it }
    guessImageMimeTypeFromBytes(imageBytes)?.let { return it }
    return "image/jpeg"
}

private fun guessImageMimeTypeFromName(imageUrl: String): String? =
    imageUrl
        .substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
        .let { extension ->
            when (extension) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "bmp" -> "image/bmp"
                "svg", "svgz" -> "image/svg+xml"
                "avif" -> "image/avif"
                "heic" -> "image/heic"
                "heif" -> "image/heif"
                else -> null
            }
        }

private fun guessImageMimeTypeFromBytes(imageBytes: ByteArray): String? {
    fun matches(vararg values: Int): Boolean =
        imageBytes.size >= values.size &&
            values.indices.all { index -> imageBytes[index].toInt() and 0xff == values[index] }

    return when {
        matches(0xff, 0xd8, 0xff) -> "image/jpeg"
        matches(0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a) -> "image/png"
        matches(0x47, 0x49, 0x46, 0x38) -> "image/gif"
        matches(0x42, 0x4d) -> "image/bmp"
        imageBytes.size >= 12 &&
            matches(0x52, 0x49, 0x46, 0x46) &&
            imageBytes[8].toInt().toChar() == 'W' &&
            imageBytes[9].toInt().toChar() == 'E' &&
            imageBytes[10].toInt().toChar() == 'B' &&
            imageBytes[11].toInt().toChar() == 'P' -> "image/webp"

        else -> null
    }
}

fun Context.hasImageExportPermission(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
        PackageManager.PERMISSION_GRANTED

fun Context.requestImageExportPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        val activity = this as? Activity ?: return
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            1001,
        )
    }
}

fun Context.loadExportAssetText(fileName: String): String =
    assets.open(fileName).use { inputStream ->
        inputStream.bufferedReader().use { reader ->
            reader.readText()
        }
    }
