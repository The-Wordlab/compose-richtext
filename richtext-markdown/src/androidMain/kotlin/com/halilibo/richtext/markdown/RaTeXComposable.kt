package com.halilibo.richtext.markdown

import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.ratex.RaTeXEngine
import io.ratex.RaTeXFontLoader
import io.ratex.RaTeXRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders a block-level LaTeX formula centered horizontally using RaTeX.
 */
@Composable
internal fun RaTeXBlockFormula(
  latex: String,
  textColor: Color,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  val fontSizePx = with(density) { 18.dp.toPx() }
  val colorArgb = textColor.toArgb()

  LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
      RaTeXFontLoader.ensureLoaded(context)
    }
  }

  val cache = LocalRaTeXBitmapCache.current
  val bitmapResult by produceState<BitmapResult?>(
    initialValue = cache?.get(latex, true, fontSizePx, colorArgb),
    latex, fontSizePx, colorArgb
  ) {
    value = try {
      renderToBitmap(latex, displayMode = true, fontSizePx, colorArgb, cache)
    } catch (_: Exception) {
      null
    }
  }

  val result = bitmapResult
  if (result != null) {
    val widthDp = with(density) { result.size.width.toDp() }
    val heightDp = with(density) { result.size.height.toDp() }
    Box(
      modifier = modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(vertical = 16.dp),
      contentAlignment = Alignment.Center
    ) {
      BitmapCanvas(
        bitmap = result.bitmap,
        modifier = Modifier.size(widthDp, heightDp)
      )
    }
  }
}

/**
 * Renders an inline LaTeX formula using RaTeX.
 */
@Composable
internal fun RaTeXInlineFormula(
  latex: String,
  textColor: Color,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  val fontSizePx = with(density) { 14.dp.toPx() }
  val colorArgb = textColor.toArgb()

  LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
      RaTeXFontLoader.ensureLoaded(context)
    }
  }

  val cache = LocalRaTeXBitmapCache.current
  val bitmapResult by produceState<BitmapResult?>(
    initialValue = cache?.get(latex, false, fontSizePx, colorArgb),
    latex, fontSizePx, colorArgb
  ) {
    value = try {
      renderToBitmap(latex, displayMode = false, fontSizePx, colorArgb, cache)
    } catch (_: Exception) {
      null
    }
  }

  val result = bitmapResult
  if (result != null) {
    val widthDp = with(density) { result.size.width.toDp() }
    val heightDp = with(density) { result.size.height.toDp() }
    BitmapCanvas(
      bitmap = result.bitmap,
      modifier = modifier.size(widthDp, heightDp)
    )
  }
}

/**
 * Pre-rendered bitmap with its pixel dimensions.
 */
internal class BitmapResult(
  val bitmap: Bitmap,
  val size: IntSize,
)

/**
 * Renders LaTeX to a colored Bitmap on a background thread.
 * The bitmap has the color filter already applied, so drawing it
 * is a single drawBitmap() call — no saveLayer() needed.
 */
private suspend fun renderToBitmap(
  latex: String,
  displayMode: Boolean,
  fontSizePx: Float,
  colorArgb: Int,
  cache: RaTeXBitmapCache?,
): BitmapResult? {
  // Check cache first
  cache?.get(latex, displayMode, fontSizePx, colorArgb)?.let { return it }

  return withContext(Dispatchers.Default) {
    val displayList = RaTeXEngine.parse(latex, displayMode)
    val renderer = RaTeXRenderer(displayList, fontSizePx) { RaTeXFontLoader.getTypeface(it) }

    val width = maxOf(1, renderer.widthPx.toInt())
    val height = maxOf(1, renderer.totalHeightPx.toInt())

    // Render to bitmap (black on transparent)
    val rawBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val rawCanvas = android.graphics.Canvas(rawBitmap)
    renderer.draw(rawCanvas)

    // Apply color filter to create the final colored bitmap
    val coloredBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val colorCanvas = android.graphics.Canvas(coloredBitmap)
    colorCanvas.drawBitmap(rawBitmap, 0f, 0f, COLOR_PAINT_POOL.withColor(colorArgb))
    rawBitmap.recycle()

    val result = BitmapResult(coloredBitmap, IntSize(width, height))
    cache?.put(latex, displayMode, fontSizePx, colorArgb, result)
    result
  }
}

/**
 * Draws a pre-rendered bitmap. This is a single drawBitmap() call —
 * no saveLayer(), no display list iteration, no path drawing.
 * Hardware-accelerated and extremely fast.
 */
@Composable
private fun BitmapCanvas(
  bitmap: Bitmap,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    drawIntoCanvas { canvas ->
      canvas.nativeCanvas.drawBitmap(bitmap, 0f, 0f, null)
    }
  }
}

/**
 * Thread-safe Paint pool for applying color filters during bitmap rendering.
 * Avoids allocating Paint objects on every render.
 */
private object COLOR_PAINT_POOL {
  private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

  @Synchronized
  fun withColor(colorArgb: Int): android.graphics.Paint {
    paint.colorFilter = PorterDuffColorFilter(colorArgb, PorterDuff.Mode.SRC_IN)
    return paint
  }
}
