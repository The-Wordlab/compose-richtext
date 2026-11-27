package com.halilibo.richtext.markdown

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PorterDuff
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.ui.RichTextScope
import com.halilibo.richtext.ui.currentFontSizePx
import com.halilibo.richtext.ui.currentTextStyle
import io.ratex.DisplayList
import io.ratex.RaTeXEngine
import io.ratex.RaTeXFontLoader
import io.ratex.RaTeXRenderer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil

@Composable
private fun mathFontSizePx(size: TextUnit, default: Dp): Float = with(LocalDensity.current) {
  val pixels = when {
    !size.value.isFinite() || size.value <= 0f -> default.toPx()
    size.isSp -> size.toPx()
    size.isEm -> RichTextScope.currentFontSizePx * size.value
    else -> default.toPx()
  }
  pixels.takeIf { it.isFinite() && it > 0f } ?: default.toPx()
}

@Composable
internal fun RaTeXFormula(
  latex: String,
  displayMode: Boolean,
  modifier: Modifier = Modifier,
  parse: suspend (String, Boolean) -> DisplayList = { source, display -> RaTeXEngine.parse(source, display) },
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  val config = LocalLatexConfig.current
  val fontSizePx = if (displayMode) {
    mathFontSizePx(config.displayFontSize, 18.dp)
  } else {
    mathFontSizePx(config.inlineFontSize, 14.dp)
  }
  val colorArgb = config.textColor.toArgb()
  val cache = LocalRaTeXBitmapCache.current
  val rendered by produceState<FormulaResult?>(
    initialValue = cache?.get(latex, displayMode, fontSizePx, colorArgb),
    latex, displayMode, fontSizePx, colorArgb, cache, parse,
  ) {
    value = try {
      renderToBitmap(context, latex, displayMode, fontSizePx, colorArgb, cache, parse)
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: Exception) {
      null
    }
  }

  when (val result = rendered) {
    is BitmapResult -> {
      val width = with(density) { result.size.width.toDp() }
      val height = with(density) { result.size.height.toDp() }
      if (displayMode) {
        Box(
          modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 16.dp),
          contentAlignment = Alignment.Center,
        ) {
          BitmapCanvas(result.bitmap, Modifier.size(width, height))
        }
      } else {
        BitmapCanvas(
          result.bitmap,
          modifier.widthIn(max = width).horizontalScroll(rememberScrollState()).size(width, height),
        )
      }
    }
    FormulaTooLarge -> BasicText(
      text = latex,
      modifier = if (displayMode) modifier.fillMaxWidth().padding(vertical = 16.dp) else modifier,
      style = RichTextScope.currentTextStyle.copy(color = config.textColor),
    )
    null -> Unit
  }
}

internal sealed interface FormulaResult
internal object FormulaTooLarge : FormulaResult
internal class BitmapResult(val bitmap: Bitmap, val size: IntSize) : FormulaResult

// A chat formula may use up to 4 MiB of ARGB_8888 pixels, with a 4K edge limit for thin textures.
private const val MAX_BITMAP_PIXELS = 1024 * 1024
private const val MAX_BITMAP_SIDE = 4096f

internal fun formulaBitmapSize(width: Float, height: Float): IntSize? {
  if (width !in 0f..MAX_BITMAP_SIDE || height !in 0f..MAX_BITMAP_SIDE) return null
  val size = IntSize(ceil(width).toInt().coerceAtLeast(1), ceil(height).toInt().coerceAtLeast(1))
  return size.takeIf { it.width * it.height <= MAX_BITMAP_PIXELS }
}

private suspend fun renderToBitmap(
  context: Context,
  latex: String,
  displayMode: Boolean,
  fontSizePx: Float,
  colorArgb: Int,
  cache: RaTeXBitmapCache?,
  parse: suspend (String, Boolean) -> DisplayList,
): FormulaResult {
  cache?.get(latex, displayMode, fontSizePx, colorArgb)?.let { return it }
  return withContext(Dispatchers.Default) {
    RaTeXFontLoader.ensureLoaded(context)
    val displayList = parse(latex, displayMode)
    val renderer = RaTeXRenderer(displayList, fontSizePx) { RaTeXFontLoader.getTypeface(it) }
    val size = formulaBitmapSize(renderer.widthPx, renderer.totalHeightPx)
    val result = if (size == null) {
      FormulaTooLarge
    } else {
      val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
      val canvas = android.graphics.Canvas(bitmap)
      renderer.draw(canvas)
      canvas.drawColor(colorArgb, PorterDuff.Mode.SRC_IN)
      BitmapResult(bitmap, size)
    }
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
