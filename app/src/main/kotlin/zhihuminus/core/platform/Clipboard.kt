package com.zhihuminus.core.platform

import android.content.ClipData
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalContext

val Context.clipboardManager: android.content.ClipboardManager
    get() = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager

suspend fun Clipboard.copyText(text: String) {
    setClipEntry(
        ClipEntry(
            ClipData.newPlainText("text", text),
        ),
    )
}

fun Context.copyPlainText(
    label: CharSequence = "text",
    text: CharSequence?,
) {
    clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text ?: ""))
}

@Composable
fun rememberPlainTextClipboard(): (label: String, text: String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { label, text ->
            context.copyPlainText(label, text)
        }
    }
}
