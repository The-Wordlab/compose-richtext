package com.halilibo.richtext.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

/**
 * Draws the last [characters] of the rendered content ramping from the content colour down to
 * [minAlpha] at the very end, so text that is still arriving appears instead of popping in.
 */
@Immutable
public data class RichTextFadeIn(
  public val characters: Int,
  public val minAlpha: Float = 0f,
)

public val LocalRichTextFadeIn: ProvidableCompositionLocal<RichTextFadeIn?> =
  compositionLocalOf { null }

/**
 * How many rendered characters follow the current block. Markdown splits one reply across many
 * blocks, so the ramp can only stay continuous across a paragraph break if each block knows how
 * far from the end it sits.
 */
public val LocalRichTextTailOffset: ProvidableCompositionLocal<Int> = compositionLocalOf { 0 }
