/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.zhihuminus.ui.subscreens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zhihuminus.platform.androidUserMessageSink
import java.io.File

@Composable
fun WebViewCustomFontSettings(
    customFontName: String?,
    onCustomFontNameChange: (String?) -> Unit,
) {
    val context = LocalContext.current
    val userMessages = androidUserMessageSink(context)
    val fontFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString()
        val destFile = File(context.filesDir, "custom_font")
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        }
        onCustomFontNameChange(name)
        userMessages.showShortMessage("字体已设置，重新打开文章后生效")
    }
    Column(
        modifier = Modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    fontFilePicker.launch(arrayOf("font/ttf", "font/otf", "application/octet-stream"))
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("选择")
            }
            if (customFontName != null) {
                OutlinedButton(
                    onClick = {
                        File(context.filesDir, "custom_font").delete()
                        onCustomFontNameChange(null)
                        userMessages.showShortMessage("已清除自定义字体")
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("清除")
                }
            }
        }
    }
}
