package com.zhihuminus.core.util

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

fun JsonObject?.booleanCompat(vararg keys: String): Boolean = this != null &&
    keys.firstNotNullOfOrNull { key ->
        get(key)?.jsonPrimitive?.booleanOrNull
    } ?: false
