package com.zhihuminus.core.platform

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.zhihuminus.core.error.reportFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets

private const val MARKDOWN_MIME_TYPE = "text/markdown"

private data class PendingMarkdownFile(
    val fileName: String,
    val content: String,
)

/**
 * 通过 SAF（`ACTION_CREATE_DOCUMENT`）让用户选择保存位置后写入 Markdown 文件。
 *
 * 不申请任何存储权限，API 28 及以上均可用；返回的函数会先用建议文件名打开系统文件选择器，
 * 用户确认后再把内容以 UTF-8 写入其选择的 Uri。
 */
@Composable
fun rememberMarkdownFileSaver(): (fileName: String, content: String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    var pending by remember { mutableStateOf<PendingMarkdownFile?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(MARKDOWN_MIME_TYPE),
    ) { uri ->
        val request = pending
        pending = null
        if (uri == null || request == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val outputStream = context.contentResolver.openOutputStream(uri)
                        ?: throw IllegalStateException("Failed to open output stream")
                    outputStream.use { stream ->
                        stream.write(request.content.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                userMessages.showShortMessage("Markdown 已导出")
            } catch (e: Exception) {
                val error = reportFailure("MarkdownFileSaver", "export markdown", e, userAction = "导出")
                userMessages.showError(error.userMessage)
            }
        }
    }

    return { fileName, content ->
        pending = PendingMarkdownFile(fileName, content)
        launcher.launch(fileName)
    }
}
