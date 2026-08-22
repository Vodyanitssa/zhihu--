package com.zhihuminus.navigation

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import com.zhihuminus.feature.post.PostType

/**
 * Custom NavType for [PostType] enum.
 */
object PostTypeNavType : NavType<PostType>(false) {
    override fun put(bundle: SavedState, key: String, value: PostType) {
        bundle.write { putString(key, value.name) }
    }

    override fun get(bundle: SavedState, key: String): PostType? = bundle.read {
        if (!contains(key) || isNull(key)) {
            null
        } else {
            getString(key).let { name -> PostType.entries.find { it.name == name } }
        }
    }

    override fun parseValue(value: String): PostType =
        PostType.entries.find { it.name == value } ?: PostType.Article

    override fun serializeAsValue(value: PostType): String = value.name
}
