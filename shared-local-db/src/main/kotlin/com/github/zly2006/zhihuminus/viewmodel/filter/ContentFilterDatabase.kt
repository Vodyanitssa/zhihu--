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

package com.zhihuminus.viewmodel.filter

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabase.Builder
import com.zhihuminus.data.applyPlatformDriver
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [ContentViewRecord::class, BlockedKeyword::class, BlockedUser::class, BlockedQuestionAuthor::class, BlockedTopic::class, BlockedFeedRecord::class, ContentOpenEvent::class],
    version = 1,
    exportSchema = false,
)
abstract class ContentFilterDatabase : RoomDatabase() {
    abstract fun contentFilterDao(): ContentFilterDao

    abstract fun contentOpenEventDao(): ContentOpenEventDao

    abstract fun blockedKeywordDao(): BlockedKeywordDao

    abstract fun blockedUserDao(): BlockedUserDao

    abstract fun blockedQuestionAuthorDao(): BlockedQuestionAuthorDao

    abstract fun blockedTopicDao(): BlockedTopicDao

    abstract fun blockedFeedRecordDao(): BlockedFeedRecordDao
}

private const val CONTENT_FILTER_DATABASE_NAME = "content_filter_database"

@Volatile
private var contentFilterDatabase: ContentFilterDatabase? = null

fun getContentFilterDatabase(context: Context): ContentFilterDatabase =
    contentFilterDatabase ?: synchronized(ContentFilterDatabase::class) {
        contentFilterDatabase ?: buildContentFilterDatabase(
            Room.databaseBuilder(
                context.applicationContext,
                ContentFilterDatabase::class.java,
                CONTENT_FILTER_DATABASE_NAME,
            ),
        ).also {
            contentFilterDatabase = it
        }
    }

fun getContentFilterDatabase(): ContentFilterDatabase =
    contentFilterDatabase
        ?: error("Content filter database is not initialized")

fun buildContentFilterDatabase(
    builder: Builder<ContentFilterDatabase>,
): ContentFilterDatabase = builder
    .fallbackToDestructiveMigration(true)
    .applyPlatformDriver()
    .setQueryCoroutineContext(Dispatchers.Default)
    .build()
