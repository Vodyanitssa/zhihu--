package com.zhihuminus.core.platform

import android.content.ClipData
import android.content.Context
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import com.zhihuminus.util.clipboardManager

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
