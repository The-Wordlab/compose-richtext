package com.halilibo.richtext.markdown

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

  LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
      RaTeXFontLoader.ensureLoaded(context)
    }
  }

  val cache = LocalRaTeXDisplayListCache.current
  val rendererState by produceState<RaTeXRenderer?>(initialValue = null, latex, fontSizePx) {
    value = try {
      val displayList = if (cache != null) {
        cache.getOrParse(latex, displayMode = true)
      } else {
        RaTeXEngine.parse(latex, displayMode = true)
      }
      RaTeXRenderer(displayList, fontSizePx) { RaTeXFontLoader.getTypeface(it) }
    } catch (_: Exception) {
      null
    }
  }

  val renderer = rendererState
  if (renderer != null) {
    val widthDp = with(density) { renderer.widthPx.toDp() }
    val heightDp = with(density) { renderer.totalHeightPx.toDp() }
    Box(
      modifier = modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(vertical = 16.dp),
      contentAlignment = Alignment.Center
    ) {
      RaTeXCanvas(
        renderer = renderer,
        textColor = textColor,
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

  LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
      RaTeXFontLoader.ensureLoaded(context)
    }
  }

  val cache = LocalRaTeXDisplayListCache.current
  val rendererState by produceState<RaTeXRenderer?>(initialValue = null, latex, fontSizePx) {
    value = try {
      val displayList = if (cache != null) {
        cache.getOrParse(latex, displayMode = false)
      } else {
        RaTeXEngine.parse(latex, displayMode = false)
      }
      RaTeXRenderer(displayList, fontSizePx) { RaTeXFontLoader.getTypeface(it) }
    } catch (_: Exception) {
      null
    }
  }

  val renderer = rendererState
  if (renderer != null) {
    val widthDp = with(density) { renderer.widthPx.toDp() }
    val heightDp = with(density) { renderer.totalHeightPx.toDp() }
    RaTeXCanvas(
      renderer = renderer,
      textColor = textColor,
      modifier = modifier.size(widthDp, heightDp)
    )
  }
}

/**
 * Low-level composable that draws a [RaTeXRenderer] onto a Compose Canvas.
 * Applies [textColor] via a [PorterDuffColorFilter] with SRC_IN mode,
 * which recolors all drawn pixels from the default black to the desired color.
 */
@Composable
private fun RaTeXCanvas(
  renderer: RaTeXRenderer,
  textColor: Color,
  modifier: Modifier = Modifier,
) {
  val colorArgb = textColor.toArgb()
  Canvas(modifier = modifier) {
    drawIntoCanvas { canvas ->
      val nativeCanvas = canvas.nativeCanvas
      val paint = android.graphics.Paint().apply {
        colorFilter = PorterDuffColorFilter(colorArgb, PorterDuff.Mode.SRC_IN)
      }
      nativeCanvas.saveLayer(null, paint)
      renderer.draw(nativeCanvas)
      nativeCanvas.restore()
    }
  }
}
