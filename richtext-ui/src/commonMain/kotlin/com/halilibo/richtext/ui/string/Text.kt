package com.halilibo.richtext.ui.string

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.halilibo.richtext.ui.LocalRichTextFadeIn
import com.halilibo.richtext.ui.LocalRichTextTailOffset
import com.halilibo.richtext.ui.RichTextFadeIn
import com.halilibo.richtext.ui.RichTextScope
import com.halilibo.richtext.ui.Text
import com.halilibo.richtext.ui.currentContentColor
import com.halilibo.richtext.ui.currentRichTextStyle
import com.halilibo.richtext.ui.currentTextStyle
import com.halilibo.richtext.ui.string.RichTextString.Format
import kotlin.math.min

/**
 * Renders a [RichTextString] as created with [richTextString].
 *
 * @sample com.halilibo.richtext.ui.previews.TextPreview
 */
@Composable
public fun RichTextScope.Text(
  text: RichTextString,
  modifier: Modifier = Modifier,
  onTextLayout: (TextLayoutResult) -> Unit = {},
  softWrap: Boolean = true,
  overflow: TextOverflow = TextOverflow.Clip,
  maxLines: Int = Int.MAX_VALUE
) {
  val style = currentRichTextStyle.stringStyle
  val contentColor = currentContentColor
  val fadeIn = LocalRichTextFadeIn.current
  val tailOffset = LocalRichTextTailOffset.current
  val fadeColor = currentTextStyle.color.takeOrElse { contentColor }
  val annotated = remember(text, style, contentColor, fadeIn, tailOffset, fadeColor) {
    val resolvedStyle = (style ?: RichTextStringStyle.Default).resolveDefaults()
    text.toAnnotatedString(resolvedStyle, contentColor).fadeTrailing(fadeIn, tailOffset, fadeColor)
  }

  val inlineContents = remember(text) { text.getInlineContents() }
  val backgrounds = remember(text, style) {
    text.getSpanBackgrounds((style ?: RichTextStringStyle.Default).resolveDefaults())
  }
  var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
  val backgroundModifier = if (backgrounds.isEmpty()) {
    Modifier
  } else {
    Modifier.drawBehind { layout?.let { drawSpanBackgrounds(it, backgrounds) } }
  }
  val onLayout: (TextLayoutResult) -> Unit = {
    layout = it
    onTextLayout(it)
  }

  if (inlineContents.isEmpty()) {
    Text(
      text = annotated,
      modifier = backgroundModifier,
      onTextLayout = onLayout,
      softWrap = softWrap,
      overflow = overflow,
      maxLines = maxLines
    )
  } else {
    // expensive constraints reading path
    BoxWithConstraints(modifier = modifier) {
      val inlineTextContents = manageInlineTextContents(
        inlineContents = inlineContents,
        textConstraints = constraints
      )

      Text(
        text = annotated,
        modifier = backgroundModifier,
        onTextLayout = onLayout,
        inlineContent = inlineTextContents,
        softWrap = softWrap,
        overflow = overflow,
        maxLines = maxLines,
      )
    }
  }
}

internal fun AnnotatedString.fadeTrailing(
  fadeIn: RichTextFadeIn?,
  tailOffset: Int,
  color: Color
): AnnotatedString {
  if (fadeIn == null || tailOffset >= fadeIn.characters) return this
  val end = length
  val fadedCount = min(end, fadeIn.characters - tailOffset)
  if (fadedCount <= 0) return this
  return buildAnnotatedString {
    append(this@fadeTrailing)
    var clusterEnd = end
    while (clusterEnd > end - fadedCount) {
      val clusterStart = text.clusterStartBefore(clusterEnd)
      val distance = tailOffset + (end - clusterEnd)
      val alpha = fadeIn.minAlpha + (1f - fadeIn.minAlpha) * distance / fadeIn.characters
      addStyle(SpanStyle(color = color.copy(alpha = alpha)), clusterStart, clusterEnd)
      clusterEnd = clusterStart
    }
  }
}

private fun String.clusterStartBefore(end: Int): Int {
  var start = end - 1
  while (start > 0 && splitsACharacter(start)) start--
  return start
}

private fun String.splitsACharacter(index: Int): Boolean {
  val previous = this[index - 1]
  val next = this[index]
  if (previous.isHighSurrogate() && next.isLowSurrogate()) return true
  if (previous == ZERO_WIDTH_JOINER || next == ZERO_WIDTH_JOINER) return true
  if (next == VARIATION_SELECTOR || next == KEYCAP) return true
  val nextCodePoint = codePointFrom(index)
  return nextCodePoint in SKIN_TONES ||
    nextCodePoint in TAGS ||
    (nextCodePoint in REGIONAL_INDICATORS && regionalIndicatorsBefore(index) % 2 == 1)
}

private fun String.regionalIndicatorsBefore(index: Int): Int {
  var count = 0
  var position = index
  while (position > 0 && codePointUpTo(position) in REGIONAL_INDICATORS) {
    count++
    position -= REGIONAL_INDICATOR_UNITS
  }
  return count
}

private fun String.codePointFrom(index: Int): Int {
  val high = this[index]
  val low = getOrNull(index + 1)
  return if (high.isHighSurrogate() && low != null && low.isLowSurrogate()) codePoint(high, low) else high.code
}

private fun String.codePointUpTo(index: Int): Int {
  val low = this[index - 1]
  val high = getOrNull(index - 2)
  return if (low.isLowSurrogate() && high != null && high.isHighSurrogate()) codePoint(high, low) else low.code
}

private fun codePoint(high: Char, low: Char): Int =
  SUPPLEMENTARY_START + ((high.code - HIGH_SURROGATE_START) shl SURROGATE_BITS) + (low.code - LOW_SURROGATE_START)

private const val ZERO_WIDTH_JOINER = '\u200D'
private const val VARIATION_SELECTOR = '\uFE0F'
private const val KEYCAP = '\u20E3'
private const val SUPPLEMENTARY_START = 0x10000
private const val HIGH_SURROGATE_START = 0xD800
private const val LOW_SURROGATE_START = 0xDC00
private const val SURROGATE_BITS = 10
private val SKIN_TONES = 0x1F3FB..0x1F3FF
private val REGIONAL_INDICATORS = 0x1F1E6..0x1F1FF
private const val REGIONAL_INDICATOR_UNITS = 2
private val TAGS = 0xE0020..0xE007F

private fun AnnotatedString.getConsumableAnnotations(textFormatObjects: Map<String, Any>, offset: Int): Sequence<Format.Link> =
  getStringAnnotations(Format.FormatAnnotationScope, offset, offset)
    .asSequence()
    .mapNotNull {
      Format.findTag(
        it.item,
        textFormatObjects
      ) as? Format.Link
    }
