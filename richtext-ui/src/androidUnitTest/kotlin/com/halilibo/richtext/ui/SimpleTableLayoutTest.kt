package com.halilibo.richtext.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28, 34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SimpleTableLayoutTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun `GIVEN zero width WHEN measuring multiple columns THEN cells receive zero width`() {
    val measurements = Measurements()
    composeRule.setContent {
      MeasuredTable(width = 0, columns = 3, spacing = 1f, measurements = measurements)
    }

    composeRule.runOnIdle {
      assertEquals(IntSize(0, 33), measurements.tableSize)
      assertEquals(List(6) { 0 }, measurements.cellConstraints.map { it.maxWidth })
      assertTrue(measurements.cellConstraints.all { it.minWidth == 0 })
    }
  }

  @Test
  fun `GIVEN the exact spacing budget WHEN measuring THEN zero width cells preserve configured spacing`() {
    val measurements = Measurements()
    composeRule.setContent {
      MeasuredTable(width = 8, columns = 3, spacing = 2f, measurements = measurements)
    }

    composeRule.runOnIdle {
      assertEquals(IntSize(8, 36), measurements.tableSize)
      assertEquals(List(6) { 0 }, measurements.cellConstraints.map { it.maxWidth })
      assertEquals(listOf(1f, 3f, 5f, 7f), requireNotNull(measurements.decorations).columnOffsets)
    }
  }

  @Test
  fun `GIVEN narrow width WHEN fractional spacing exceeds it THEN column widths stay nonnegative`() {
    val measurements = Measurements()
    composeRule.setContent {
      MeasuredTable(width = 1, columns = 5, spacing = 0.75f, measurements = measurements)
    }

    composeRule.runOnIdle {
      assertEquals(1, measurements.tableSize.width)
      assertEquals(List(10) { 0 }, measurements.cellConstraints.map { it.maxWidth })
      assertTrue(requireNotNull(measurements.decorations).columnOffsets.zipWithNext().all { (a, b) -> a <= b })
    }
  }

  @Test
  fun `GIVEN oversized spacing WHEN measuring one or several columns THEN every cell still measures`() {
    val measurements = Measurements()
    var columns by mutableIntStateOf(1)
    var spacing by mutableFloatStateOf(512f)
    composeRule.setContent {
      MeasuredTable(width = 20, columns = columns, spacing = spacing, measurements = measurements)
    }
    composeRule.runOnIdle {
      assertEquals(List(2) { 0 }, measurements.cellConstraints.map { it.maxWidth })
      assertEquals(20, measurements.tableSize.width)
      columns = 4
      spacing = 12.5f
    }

    composeRule.runOnIdle {
      assertEquals(List(8) { 0 }, measurements.cellConstraints.map { it.maxWidth })
      assertEquals(20, measurements.tableSize.width)
    }
  }

  @Test
  fun `GIVEN ordinary width WHEN measuring a table THEN cell sizes and border positions are preserved`() {
    val measurements = Measurements()
    composeRule.setContent {
      MeasuredTable(width = 100, columns = 3, spacing = 2f, measurements = measurements)
    }

    composeRule.runOnIdle {
      assertEquals(IntSize(100, 36), measurements.tableSize)
      assertEquals(List(6) { 31 }, measurements.cellConstraints.map { it.maxWidth })
      assertEquals(
        listOf(
          Offset(2f, 2f), Offset(35f, 2f), Offset(67f, 2f),
          Offset(2f, 14f), Offset(35f, 14f), Offset(67f, 14f)
        ),
        measurements.positions
      )
      val decorations = requireNotNull(measurements.decorations)
      assertEquals(listOf(1f, 13f, 35f), decorations.rowOffsets)
      assertEquals(4, decorations.columnOffsets.size)
      listOf(1f, 33.666668f, 66.333336f, 99f).zip(decorations.columnOffsets).forEach { (expected, actual) ->
        assertEquals(expected, actual, 0.0001f)
      }
    }
  }

  @Test
  fun `GIVEN a rendered RichText table WHEN width shrinks and recovers THEN all table content returns`() {
    var width by mutableIntStateOf(240)
    var tableSize = IntSize.Zero
    composeRule.setContent {
      BoundedWidth(width) {
        BasicRichText(style = RichTextStyle(tableStyle = TableStyle(cellPadding = 0.sp, borderStrokeWidth = 1.25f))) {
          Table(
            modifier = Modifier.onSizeChanged { tableSize = it },
            headerRow = {
              cell { Text("First") }
              cell { Text("Second") }
              cell { Text("Third") }
            }
          ) {
            row {
              cell { Text("Alpha") }
              cell { Text("Beta") }
              cell { Text("Gamma") }
            }
          }
        }
      }
    }
    listOf("First", "Second", "Third", "Alpha", "Beta", "Gamma").forEach {
      composeRule.onNodeWithText(it).assertIsDisplayed()
    }
    val initialSize = composeRule.runOnIdle { tableSize }
    composeRule.runOnIdle { width = 0 }
    composeRule.runOnIdle { assertEquals(0, tableSize.width) }
    composeRule.runOnIdle { width = 2 }
    composeRule.runOnIdle { assertEquals(2, tableSize.width) }
    composeRule.runOnIdle { width = 240 }

    composeRule.runOnIdle { assertEquals(initialSize, tableSize) }
    listOf("First", "Second", "Third", "Alpha", "Beta", "Gamma").forEach {
      composeRule.onNodeWithText(it).assertIsDisplayed()
    }
  }

  @Composable
  private fun MeasuredTable(width: Int, columns: Int, spacing: Float, measurements: Measurements) {
    BoundedWidth(width) {
      SimpleTableLayout(
        columns = columns,
        rows = List(2) { row ->
          List(columns) { column ->
            @Composable {
              val index = row * columns + column
              Layout(
                content = {},
                modifier = Modifier.onPlaced { measurements.positions[index] = it.positionInParent() }
              ) { _, constraints ->
                measurements.record(index, constraints)
                layout(minOf(20, constraints.maxWidth), (row + 1) * 10) {}
              }
            }
          }
        },
        drawDecorations = {
          measurements.decorations = it
          Modifier
        },
        cellSpacing = spacing,
        modifier = Modifier.onSizeChanged { measurements.tableSize = it }
      )
    }
  }

  @Composable
  private fun BoundedWidth(width: Int, content: @Composable () -> Unit) {
    Layout(content = content) { measurables, constraints ->
      val child = measurables.single().measure(Constraints(maxWidth = width))
      layout(constraints.constrainWidth(width), constraints.constrainHeight(child.height)) {
        child.place(0, 0)
      }
    }
  }

  private class Measurements {
    val cellConstraints = mutableListOf<Constraints>()
    val positions = mutableListOf<Offset>()
    var tableSize = IntSize.Zero
    var decorations: TableLayoutResult? = null

    fun record(index: Int, constraints: Constraints) {
      while (cellConstraints.size <= index) {
        cellConstraints.add(Constraints())
        positions.add(Offset.Zero)
      }
      cellConstraints[index] = constraints
    }
  }
}
