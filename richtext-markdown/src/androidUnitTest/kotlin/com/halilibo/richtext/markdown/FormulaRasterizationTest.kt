package com.halilibo.richtext.markdown

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import io.ratex.DisplayItem
import io.ratex.DisplayList
import io.ratex.RaTeXColor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.ceil

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FormulaRasterizationTest {
  @get:Rule
  val composeRule = createComposeRule()

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `a cancelled render cannot clear the cached formula restored at the original size`() {
    val latex = "x"
    val cache = RaTeXBitmapCache().apply {
      val bitmap = Bitmap.createBitmap(14, 14, Bitmap.Config.ARGB_8888)
      put(latex, false, 14f, Color.Black.toArgb(), BitmapResult(bitmap, IntSize(14, 14)))
    }
    val started = CompletableDeferred<Unit>()
    val cancelling = CompletableDeferred<Unit>()
    val release = CompletableDeferred<Unit>()
    val finished = CompletableDeferred<Unit>()
    val parse: suspend (String, Boolean) -> DisplayList = { _, _ ->
      // The parent owns produceState's write after the background render returns.
      requireNotNull(currentCoroutineContext().job.parent).invokeOnCompletion { finished.complete(Unit) }
      started.complete(Unit)
      try {
        awaitCancellation()
      } finally {
        withContext(NonCancellable) {
          cancelling.complete(Unit)
          release.await()
        }
      }
    }
    var size by mutableStateOf(14.sp)
    composeRule.setContent {
      CompositionLocalProvider(
        LocalDensity provides Density(1f),
        LocalLatexConfig provides LatexConfig(inlineFontSize = size),
        LocalRaTeXBitmapCache provides cache,
      ) {
        RaTeXFormula(latex, displayMode = false, modifier = Modifier.testTag("formula"), parse = parse)
      }
    }
    val formula = composeRule.onNodeWithTag("formula")
    formula.assertExists()
    try {
      composeRule.runOnIdle { size = 28.sp }
      composeRule.waitForIdle()
      composeRule.waitUntil(5_000) { started.isCompleted }
      composeRule.runOnIdle { size = 14.sp }
      composeRule.waitForIdle()
      composeRule.waitUntil(5_000) { cancelling.isCompleted }
      formula.assertExists()
    } finally {
      release.complete(Unit)
    }
    composeRule.waitUntil(5_000) { finished.isCompleted }
    composeRule.waitForIdle()
    formula.assertExists()
  }

  @Test
  fun `an uncached formula rasterizes at the configured font scale`() {
    val latex = "\\rule{1em}{0.5em}"
    val rule = DisplayList(
      width = 1.0, height = 0.5, depth = 0.0,
      items = listOf(DisplayItem.Rect(0.0, 0.0, 1.0, 0.5, RaTeXColor(0f, 0f, 0f, 1f))),
    )
    val config = LatexConfig(textColor = Color.Blue, inlineFontSize = 12.sp)
    val color = config.textColor.toArgb()
    val cache = RaTeXBitmapCache()
    var fontScale by mutableFloatStateOf(1f)
    composeRule.setContent {
      CompositionLocalProvider(
        LocalDensity provides Density(1f, fontScale),
        LocalLatexConfig provides config,
        LocalRaTeXBitmapCache provides cache,
      ) {
        RaTeXFormula(latex, displayMode = false, parse = { _, _ -> rule })
      }
    }

    for (scale in listOf(1f, 2f)) {
      composeRule.runOnIdle { fontScale = scale }
      composeRule.waitForIdle()
      val pixels = with(Density(1f, scale)) { config.inlineFontSize.toPx() }
      composeRule.waitUntil(5_000) { cache.get(latex, false, pixels, color) is BitmapResult }
      val bitmap = (cache.get(latex, false, pixels, color) as BitmapResult).bitmap
      assertEquals(ceil(pixels).toInt(), bitmap.width)
      assertEquals(ceil(pixels / 2).toInt(), bitmap.height)
      assertEquals(color, bitmap.getPixel(bitmap.width - 1, bitmap.height / 2))
    }
  }
}
