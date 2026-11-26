package com.halilibo.richtext.markdown

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.halilibo.richtext.ui.BasicRichText
import com.halilibo.richtext.ui.RichTextThemeProvider
import com.halilibo.richtext.ui.string.InlineContent
import com.halilibo.richtext.ui.string.Text
import com.halilibo.richtext.ui.string.richTextString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MathFontSizeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun `bitmap dimensions round outwards and reject oversized or non-finite allocations`() {
    assertEquals(IntSize(121, 36), formulaBitmapSize(120.25f, 35.5f))
    assertEquals(IntSize(4096, 256), formulaBitmapSize(4096f, 256f))
    assertNull(formulaBitmapSize(4096f, 256.1f))
    assertNull(formulaBitmapSize(4097f, 1f))
    assertNull(formulaBitmapSize(1f, Float.NaN))
  }

  @Test
  fun `invalid sizes use the existing defaults and oversized formulas show their source`() {
    val cache = RaTeXBitmapCache().apply {
      put("inline source", false, 14f, Color.Black.toArgb(), FormulaTooLarge)
      put("display source", true, 18f, Color.Black.toArgb(), FormulaTooLarge)
    }
    val config = LatexConfig(inlineFontSize = (-1).em, displayFontSize = Float.POSITIVE_INFINITY.sp)
    composeRule.setContent {
      CompositionLocalProvider(
        LocalLatexConfig provides config,
        LocalRaTeXBitmapCache provides cache,
        LocalDensity provides Density(1f),
      ) {
        BasicRichText {
          InlineMathContent("inline source")
          DisplayMathContent("display source")
        }
      }
    }
    composeRule.onNodeWithText("inline source").assertExists()
    composeRule.onNodeWithText("display source").assertExists()
  }

  @Test
  fun `an inline formula wider than its paragraph scrolls to the final glyphs`() {
    val cache = RaTeXBitmapCache().apply { storeBitmap(false, 14f, 120) }
    val text = richTextString {
      appendInlineContent(content = InlineContent { InlineMathContent("x") })
    }
    var placeholderWidth = 0f
    composeRule.setContent {
      CompositionLocalProvider(LocalRaTeXBitmapCache provides cache, LocalDensity provides Density(1f)) {
        BasicRichText(Modifier.width(80.dp)) {
          Text(text, onTextLayout = { placeholderWidth = it.placeholderRects.single()?.width ?: 0f })
        }
      }
    }
    val formula = composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
    composeRule.waitUntil { placeholderWidth == 80f }
    val range = formula.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange]
    assertEquals(40f, range.maxValue(), 0f)
    formula.performSemanticsAction(SemanticsActions.ScrollBy) { it(40f, 0f) }
    composeRule.waitUntil { range.value() == 40f }
  }

  @Test
  fun `inline placeholders and display math remeasure at the new font scale`() {
    val cache = RaTeXBitmapCache()
    val config = LatexConfig(inlineFontSize = 1.2.em, displayFontSize = 1.5.em)
    val bodySize = 16.sp
    for (scale in 1..2) {
      val bodyPx = with(Density(1f, scale.toFloat())) { bodySize.toPx() }
      cache.storeBitmap(false, bodyPx * 1.2f, 24 * scale)
      cache.storeBitmap(true, bodyPx * 1.5f, 30 * scale)
    }
    val text = richTextString {
      append("Value ")
      appendInlineContent(content = InlineContent { InlineMathContent("x") })
    }
    var fontScale by mutableFloatStateOf(1f)
    var inlineWidth = 0f
    var blockHeight = 0

    composeRule.setContent {
      CompositionLocalProvider(
        LocalLatexConfig provides config,
        LocalRaTeXBitmapCache provides cache,
        LocalDensity provides Density(1f, fontScale),
      ) {
        RichTextThemeProvider(textStyleProvider = { TextStyle(fontSize = bodySize) }) {
          BasicRichText {
            Text(text, onTextLayout = { inlineWidth = it.placeholderRects.single()?.width ?: 0f })
            RaTeXFormula("x", displayMode = true, Modifier.onSizeChanged { blockHeight = it.height })
          }
        }
      }
    }

    composeRule.waitUntil { inlineWidth in 23f..25f && blockHeight == 30 + 32 }
    composeRule.runOnIdle { fontScale = 2f }
    composeRule.waitUntil { inlineWidth in 47f..49f && blockHeight == 60 + 32 }
  }

  private fun RaTeXBitmapCache.storeBitmap(display: Boolean, fontSizePx: Float, side: Int) {
    val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
    put("x", display, fontSizePx, Color.Black.toArgb(), BitmapResult(bitmap, IntSize(side, side)))
  }
}
