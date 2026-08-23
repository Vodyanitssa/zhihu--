package com.zhihuminus.core.platform

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

suspend fun Clipboard.copyText(text: String) {
    setClipEntry(
        ClipEntry(
            ClipData.newPlainText("text", text),
        ),
    )
}
