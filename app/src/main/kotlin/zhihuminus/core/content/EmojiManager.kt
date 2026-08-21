package com.zhihuminus.core.content

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.em
import kotlinx.serialization.json.Json

object EmojiManager {
    var mapping: Map<String, String> = emptyMap()
    var inlineContent: Map<String, InlineTextContent> = emptyMap()

    private val json = Json { ignoreUnknownKeys = true }

    fun initialize(context: Context) {
        if (mapping.isNotEmpty()) return
        val jsonString = context.assets
            .open("emojis/emoji_mapping.json")
            .bufferedReader()
            .use { it.readText() }
        mapping = json.decodeFromString<Map<String, String>>(jsonString)
        inlineContent = mapping.mapValues { (name, resource) ->
            val fileName = "emojis/images/$resource"
            val bitmap = context.assets.open(fileName).use { inputStream ->
                android.graphics.BitmapFactory.decodeStream(inputStream)
            }
            InlineTextContent(
                placeholder = Placeholder(
                    width = 1.3.em,
                    height = 1.3.em,
                    placeholderVerticalAlign =
                        PlaceholderVerticalAlign.TextCenter,
                ),
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = name,
                        modifier = Modifier,
                    )
                } else {
                    Text(text = name)
                }
            }
        }
    }
}
