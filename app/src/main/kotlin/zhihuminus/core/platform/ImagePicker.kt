package com.zhihuminus.core.platform

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import java.io.IOException

private const val MAX_IMAGE_BYTES = 10 * 1024 * 1024

/**
 * 相册图片选择器（Android Photo Picker）。
 *
 * 返回的启动函数只负责拉起系统选择器；选中后在主线程读取字节并通过 [onPicked] 回调。
 * 读取失败或超过 10 MB 时通过 [onError] 提示。
 */
@Composable
fun rememberImagePicker(
    onPicked: (bytes: ByteArray, mimeType: String?) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    val context = LocalContext.current.applicationContext
    val currentOnPicked by rememberUpdatedState(onPicked)
    val currentOnError by rememberUpdatedState(onError)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching { readImageBytes(context, uri) }
            .onSuccess { bytes ->
                if (bytes.size > MAX_IMAGE_BYTES) {
                    currentOnError("图片不能超过 10 MB")
                } else {
                    currentOnPicked(bytes, context.contentResolver.getType(uri))
                }
            }.onFailure {
                currentOnError("读取图片失败: ${it.message ?: "未知错误"}")
            }
    }
    return remember(launcher) {
        { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    }
}

private fun readImageBytes(context: Context, uri: Uri): ByteArray =
    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: throw IOException("无法打开所选图片")
