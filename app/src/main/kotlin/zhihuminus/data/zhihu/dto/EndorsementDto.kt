package com.zhihuminus.data.zhihu.dto

import kotlinx.serialization.Serializable

@Serializable
data class EndorsementDto(
    val actionUrl: String? = null,
    val backgroundColor: EndorsementColorDto? = null,
    val elements: List<EndorsementElementDto>? = null,
)

@Serializable
data class EndorsementElementDto(
    val type: String? = null,
    val content: String? = null,
    val fontColor: EndorsementColorDto? = null,
    val imageKey: String? = null,
    val imageColor: EndorsementColorDto? = null,
    val selectedImageKey: String? = null,
)

@Serializable
data class EndorsementColorDto(
    val alpha: Float = 1f,
    val group: String? = null,
)
