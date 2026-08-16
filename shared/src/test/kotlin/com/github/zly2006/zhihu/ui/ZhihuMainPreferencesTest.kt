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

package com.github.zly2006.zhihu.ui

import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Daily
import com.github.zly2006.zhihu.navigation.Follow
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.HotList
import com.github.zly2006.zhihu.navigation.MyCollections
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.ui.subscreens.bottomBarItemOrderFromPreference
import com.github.zly2006.zhihu.ui.subscreens.defaultBottomBarSelectionKeys
import com.github.zly2006.zhihu.ui.subscreens.normalizeBottomBarItemOrder
import com.github.zly2006.zhihu.ui.subscreens.normalizeBottomBarSelection
import com.github.zly2006.zhihu.ui.subscreens.resolveValidStartDestinationKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZhihuMainPreferencesTest {
    @Test
    fun defaultBottomBarSelectionKeys() {
        assertEquals(
            linkedSetOf(Home.name, Follow.name, Daily.name),
            defaultBottomBarSelectionKeys(),
        )
    }

    @Test
    fun normalizeBottomBarSelectionKeepsAccountWhenHomeIsAbsent() {
        val normalized = normalizeBottomBarSelection(
            selectedKeys = linkedSetOf(Follow.name, Daily.name, HotList.name, OnlineHistory.name),
        )

        assertEquals(5, normalized.size)
        assertTrue(Account.name in normalized)
        assertFalse(Home.name in normalized)
    }

    @Test
    fun normalizeBottomBarSelectionReplacesAccountWithHomeWhenEnabled() {
        val normalized = normalizeBottomBarSelection(
            selectedKeys = linkedSetOf(Home.name, Account.name),
            enforceMinimumSelection = true,
        )

        assertTrue(Home.name in normalized)
        assertFalse(Account.name in normalized)
        assertEquals(3, normalized.size)
    }

    @Test
    fun resolveValidStartDestinationFallsBackToFirstAvailableDestination() {
        assertEquals(
            Follow.name,
            resolveValidStartDestinationKey(
                preferredKey = HotList.name,
                availableKeysInOrder = listOf(Follow.name, Daily.name),
            ),
        )
    }

    @Test
    fun normalizeBottomBarSelectionAllowsCollectionsEntry() {
        val normalized = normalizeBottomBarSelection(
            selectedKeys = linkedSetOf(Home.name, HotList.name, MyCollections.name),
            enforceMinimumSelection = true,
        )

        assertTrue(MyCollections.name in normalized)
        assertEquals(3, normalized.size)
    }

    @Test
    fun normalizeBottomBarItemOrderKeepsPreferredOrderAndAppendsMissingSelectedItems() {
        val normalized = normalizeBottomBarItemOrder(
            preferredOrderKeys = listOf(HotList.name, MyCollections.name, HotList.name, "Unknown"),
            selectedKeys = linkedSetOf(Home.name, HotList.name, MyCollections.name, Daily.name),
        )

        assertEquals(
            listOf(HotList.name, MyCollections.name, Home.name, Daily.name),
            normalized,
        )
    }

    @Test
    fun bottomBarItemOrderFromPreferenceIgnoresUnselectedAndUnknownKeys() {
        val normalized = bottomBarItemOrderFromPreference(
            preferenceValue = "${MyCollections.name}, Unknown, ${HotList.name}, ${OnlineHistory.name}",
            selectedKeys = linkedSetOf(Home.name, HotList.name, MyCollections.name),
        )

        assertEquals(
            listOf(MyCollections.name, HotList.name, Home.name),
            normalized,
        )
    }
}
