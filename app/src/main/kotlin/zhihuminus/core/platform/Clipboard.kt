package com.zhihuminus.core.platform

import android.content.ClipData
import android.content.Context
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

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
