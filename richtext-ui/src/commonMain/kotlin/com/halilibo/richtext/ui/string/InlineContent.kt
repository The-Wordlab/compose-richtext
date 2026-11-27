@file:Suppress("RemoveEmptyParenthesesFromAnnotationEntry", "FunctionName")

package com.halilibo.richtext.ui.string

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.PlaceholderVerticalAlign.Companion.AboveBaseline
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.halilibo.richtext.ui.LocalInlineFontSizePx
import com.halilibo.richtext.ui.RichTextScope
import com.halilibo.richtext.ui.currentFontSizePx

/**
 * A Composable that can be embedded inline in a [RichTextString] by passing to
 * [RichTextString.Builder.appendInlineContent].
 *
 * @param initialSize Optional function to calculate the initial size of the content. Not specifying
 * this may cause flicker.
 * @param placeholderVerticalAlign Used to specify how a placeholder is vertically aligned within a
 * text line.
 */
public class InlineContent(
  internal val initialSize: (Density.() -> IntSize)? = null,
  internal val placeholderVerticalAlign: PlaceholderVerticalAlign = AboveBaseline,
  internal val useTextRelativeSize: Boolean,
  internal val content: @Composable Density.(alternateText: String) -> Unit
) {
  public constructor(
    initialSize: (Density.() -> IntSize)? = null,
    placeholderVerticalAlign: PlaceholderVerticalAlign = AboveBaseline,
    content: @Composable Density.(alternateText: String) -> Unit,
  ) : this(initialSize, placeholderVerticalAlign, false, content)
}

/**
 * Converts a map of [InlineContent]s into a map of [InlineTextContent] that is ready to pass to
 * the core Text composable. Whenever any of the contents resize themselves, or if the map changes,
 * a new map will be returned with updated [Placeholder]s.
 */
@Composable internal fun manageInlineTextContents(
  inlineContents: Map<String, InlineContent>,
  text: AnnotatedString,
  textConstraints: Constraints
): Map<String, InlineTextContent> {
  val density = LocalDensity.current
  val paragraphFontSize = RichTextScope.currentFontSizePx
  val fontSizes = remember(text, inlineContents, paragraphFontSize, density) {
    val spans = text.spanStyles.filter { it.item.fontSize.isSpecified }
    if (spans.isEmpty()) emptyMap() else buildMap {
      for (marker in text.getStringAnnotations(0, text.length)) {
        if (inlineContents[marker.item]?.useTextRelativeSize != true) continue
        var fontSize = paragraphFontSize
        for (span in spans) {
          if (marker.start in span.start until span.end) {
            val size = span.item.fontSize
            fontSize = if (size.isEm) fontSize * size.value else density.absoluteSpanFontSizePx(size)
          }
        }
        put(marker.item, fontSize)
      }
    }
  }

  return inlineContents.mapValues { (id, content) ->
    reifyInlineContent(
      content,
      Constraints(maxWidth = textConstraints.maxWidth, maxHeight = textConstraints.maxHeight),
      density,
      if (content.useTextRelativeSize) fontSizes[id] ?: paragraphFontSize else null,
    )
  }
}

internal expect fun Density.absoluteSpanFontSizePx(size: TextUnit): Float

/**
 * Given an [InlineContent] function, wraps it in a [InlineTextContent] that will allow the content
 * to measure itself inside the enclosing layout's maximum constraints, and automatically return a
 * new [InlineTextContent] whenever the content changes size to update how much space is reserved
 * in the text layout for the content.
 */
@Composable private fun reifyInlineContent(
  content: InlineContent,
  contentConstraints: Constraints,
  density: Density,
  emSize: Float?,
): InlineTextContent {
  var size by remember {
    mutableStateOf(
      content.initialSize?.invoke(density),
      structuralEqualityPolicy()
    )
  }

  with(density) {
    // Older Android Compose releases convert sp placeholders with linear scaledDensity even
    // when glyphs use nonlinear font scaling. Em uses the actual surrounding text paint.
    fun Int.placeholderSize() = if (emSize != null && emSize > 0f && emSize.isFinite()) (this / emSize).em else toSp()
    val placeholder = Placeholder(
      width = size?.width?.placeholderSize() ?: 0.sp,
      height = size?.height?.placeholderSize() ?: 1.sp,
      placeholderVerticalAlign = content.placeholderVerticalAlign
    )

    return InlineTextContent(placeholder) { alternateText ->
      Layout(content = {
        if (emSize == null) content.content(this, alternateText) else {
          CompositionLocalProvider(LocalInlineFontSizePx provides emSize) { content.content(this, alternateText) }
        }
      }) { measurables, _ ->
        // Measure the content with the constraints for the parent Text layout, not the actual.
        // This allows it to determine exactly how large it needs to be so we can update the
        // placeholder.
        val contentPlaceable = measurables.singleOrNull()?.measure(contentConstraints)
          ?: return@Layout layout(0, 0) {}

        if (contentPlaceable.width != size?.width
          || contentPlaceable.height != size?.height
        ) {
          size = IntSize(contentPlaceable.width, contentPlaceable.height)
        }

        layout(contentPlaceable.width, contentPlaceable.height) {
          contentPlaceable.place(0, 0)
        }
      }
    }
  }
}
