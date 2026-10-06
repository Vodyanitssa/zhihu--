package com.zhihuminus.core.settings

import com.zhihuminus.core.settings.model.AppSettings
import com.zhihuminus.core.settings.model.ShareActionOption
import com.zhihuminus.core.settings.model.StartDestinationOption
import com.zhihuminus.theme.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingsRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testDefaultSettingsWhenFileDoesNotExist() {
        val file = File(tempFolder.root, "settings.json")
        val repo = JsonAppSettingsRepository(file)

        val settings = repo.current
        assertEquals(ThemeMode.SYSTEM, settings.theme.mode)
        assertTrue(settings.theme.useDynamicColor)
        assertEquals(100, settings.reading.fontSizePercent)
        assertEquals(160, settings.reading.lineHeightPercent)
        assertEquals(StartDestinationOption.Home, settings.navigation.startDestination)
        assertTrue(settings.navigation.tapToScrollToTop)
        assertFalse(settings.navigation.autoHideBottomBar)
        assertEquals(ShareActionOption.Ask, settings.interaction.shareAction)
        assertTrue(settings.interaction.autoRefreshHomeOnStartup)
        assertTrue(settings.notification.autoMarkAsRead)
        assertTrue(settings.notification.showUnreadBadge)
    }

    @Test
    fun testUpdatePersistsAndNotifiesFlow() = runBlocking {
        val file = File(tempFolder.root, "settings.json")
        val repo = JsonAppSettingsRepository(file)

        repo.update {
            it.copy(
                reading = it.reading.copy(fontSizePercent = 120, lineHeightPercent = 180),
            )
        }

        assertEquals(120, repo.current.reading.fontSizePercent)
        assertEquals(180, repo.current.reading.lineHeightPercent)
        assertEquals(120, repo.settingsFlow.value.reading.fontSizePercent)
        assertTrue(file.exists())

        // Create new repository from the same file to verify persistence
        val reloadedRepo = JsonAppSettingsRepository(file)
        assertEquals(120, reloadedRepo.current.reading.fontSizePercent)
        assertEquals(180, reloadedRepo.current.reading.lineHeightPercent)
    }

    @Test
    fun testSubmoduleUpdates() = runBlocking {
        val file = File(tempFolder.root, "settings.json")
        val repo = JsonAppSettingsRepository(file)

        repo.update {
            it.copy(
                theme = it.theme.copy(mode = ThemeMode.DARK, useDynamicColor = false, customColor = 0xFF123456.toInt()),
                navigation = it.navigation.copy(startDestination = StartDestinationOption.Daily, autoHideBottomBar = true),
                interaction = it.interaction.copy(shareAction = ShareActionOption.CopyLink, autoRefreshHomeOnStartup = false),
                notification = it.notification.copy(autoMarkAsRead = false, showUnreadBadge = false),
            )
        }

        val updated = repo.current
        assertEquals(ThemeMode.DARK, updated.theme.mode)
        assertFalse(updated.theme.useDynamicColor)
        assertEquals(0xFF123456.toInt(), updated.theme.customColor)
        assertEquals(StartDestinationOption.Daily, updated.navigation.startDestination)
        assertTrue(updated.navigation.autoHideBottomBar)
        assertEquals(ShareActionOption.CopyLink, updated.interaction.shareAction)
        assertFalse(updated.interaction.autoRefreshHomeOnStartup)
        assertFalse(updated.notification.autoMarkAsRead)
        assertFalse(updated.notification.showUnreadBadge)
    }

    @Test
    fun testCorruptedFileFallsBackToDefault() {
        val file = File(tempFolder.root, "settings.json")
        file.writeText("INVALID JSON {[[")

        val repo = JsonAppSettingsRepository(file)
        assertEquals(AppSettings(), repo.current)
    }
}
