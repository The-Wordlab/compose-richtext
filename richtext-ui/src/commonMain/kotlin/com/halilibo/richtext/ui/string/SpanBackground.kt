package com.halilibo.richtext.ui.string

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/**
 * A rounded rectangle drawn behind every line a format covers. A [androidx.compose.ui.text.SpanStyle]
 * background is always square and as tall as the line.
 */
@Immutable
public data class SpanBackground(
  val color: Color,
  val cornerRadius: Dp,
  val horizontalOutset: Dp = 0.dp,
  val verticalInset: Dp = 0.dp,
)

internal class SpanBackgroundRange(val start: Int, val end: Int, val background: SpanBackground)

internal fun DrawScope.drawSpanBackgrounds(layout: TextLayoutResult, ranges: List<SpanBackgroundRange>) {
  val textLength = layout.layoutInput.text.length
  ranges.forEach { range ->
    val start = range.start.coerceIn(0, textLength)
    val end = range.end.coerceIn(0, textLength)
    if (end <= start) return@forEach
    val outset = range.background.horizontalOutset.toPx()
    val inset = range.background.verticalInset.toPx()
    val radius = range.background.cornerRadius.toPx()
    for (line in layout.getLineForOffset(start)..layout.getLineForOffset(end - 1)) {
      val lineStart = max(start, layout.getLineStart(line))
      val lineEnd = min(end, layout.getLineEnd(line, visibleEnd = true))
      if (lineEnd <= lineStart) continue
      val first = layout.getBoundingBox(lineStart)
      val last = layout.getBoundingBox(lineEnd - 1)
      val left = min(first.left, last.left) - outset
      val right = max(first.right, last.right) + outset
      val top = layout.getLineTop(line) + inset
      val bottom = layout.getLineBottom(line) - inset
      if (right <= left || bottom <= top) continue
      drawRoundRect(
        color = range.background.color,
        topLeft = Offset(left, top),
        size = Size(right - left, bottom - top),
        cornerRadius = CornerRadius(min(radius, (bottom - top) / 2)),
      )
    }
  }
}
