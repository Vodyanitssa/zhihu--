package com.zhihuminus.core.content.renderer

import androidx.compose.ui.unit.dp
import com.zhihuminus.core.content.ContentNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TableTest {
    private fun row(vararg cells: String): ContentNode.TableRow = ContentNode.TableRow(
        cells.map { ContentNode.TableCell(content = it, isHeader = false) },
    )

    @Test
    fun emptyRowsReturnNull() {
        assertNull(computeTableColumnLayout(emptyList()))
    }

    @Test
    fun rowsWithoutCellsReturnNull() {
        val emptyRow = ContentNode.TableRow(emptyList())
        assertNull(computeTableColumnLayout(listOf(emptyRow, emptyRow)))
    }

    @Test
    fun emptyStringCellsStillProduceColumns() {
        val layout = computeTableColumnLayout(listOf(row(""), row("")))!!
        assertEquals(1, layout.columnCount)
        assertEquals(listOf(72.dp), layout.minWidths)
    }

    @Test
    fun shortColumnUsesCompactWidth() {
        val layout = computeTableColumnLayout(listOf(row("abc")))!!
        assertEquals(1, layout.columnCount)
        assertEquals(listOf(72.dp), layout.minWidths)
        assertEquals(listOf(1.0f), layout.weights)
        assertEquals(72.dp, layout.totalMinWidth)
    }

    @Test
    fun mediumColumnUsesMediumWidth() {
        val layout = computeTableColumnLayout(listOf(row("a".repeat(7))))!!
        assertEquals(listOf(108.dp), layout.minWidths)
        assertEquals(listOf(1.6f), layout.weights)
    }

    @Test
    fun longColumnUsesWideWidth() {
        val layout = computeTableColumnLayout(listOf(row("a".repeat(25))))!!
        assertEquals(listOf(160.dp), layout.minWidths)
        assertEquals(listOf(2.4f), layout.weights)
    }

    @Test
    fun shortToMediumBoundary() {
        assertEquals(listOf(72.dp), computeTableColumnLayout(listOf(row("a".repeat(6))))!!.minWidths)
        assertEquals(listOf(108.dp), computeTableColumnLayout(listOf(row("a".repeat(7))))!!.minWidths)
    }

    @Test
    fun mediumToLongBoundary() {
        assertEquals(listOf(108.dp), computeTableColumnLayout(listOf(row("a".repeat(24))))!!.minWidths)
        assertEquals(listOf(160.dp), computeTableColumnLayout(listOf(row("a".repeat(25))))!!.minWidths)
    }

    @Test
    fun cjkCharactersCountAsDoubleWidth() {
        assertEquals(listOf(72.dp), computeTableColumnLayout(listOf(row("中文测")))!!.minWidths)
        assertEquals(listOf(108.dp), computeTableColumnLayout(listOf(row("中文测试")))!!.minWidths)
    }

    @Test
    fun visualLengthAsciiBoundary() {
        assertEquals(1, visualLength("a"))
        assertEquals(1, visualLength("\u007F"))
        assertEquals(2, visualLength("\u0080"))
        assertEquals(2, visualLength("中"))
        assertEquals(4, visualLength("中文"))
    }

    @Test
    fun raggedRowsUseMaxColumnCountAndTreatMissingCellsAsEmpty() {
        val layout = computeTableColumnLayout(
            listOf(row("a", "bb"), row("ccc")),
        )!!
        assertEquals(2, layout.columnCount)
        assertEquals(listOf(72.dp, 72.dp), layout.minWidths)
        assertEquals(144.dp, layout.totalMinWidth)
    }

    @Test
    fun longestCellInColumnDrivesWidth() {
        val layout = computeTableColumnLayout(
            listOf(row("a"), row("a".repeat(30))),
        )!!
        assertEquals(listOf(160.dp), layout.minWidths)
    }

    @Test
    fun totalMinWidthSumsAllColumns() {
        val layout = computeTableColumnLayout(
            listOf(row("a", "a".repeat(7), "a".repeat(25))),
        )!!
        assertEquals(72.dp + 108.dp + 160.dp, layout.totalMinWidth)
    }
}
