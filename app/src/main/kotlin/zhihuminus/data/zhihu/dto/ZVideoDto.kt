package com.zhihuminus.data.zhihu.dto

import kotlinx.serialization.Serializable

@Serializable
data class ZVideoDto(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val video: VideoInfoDto? = null,
)
