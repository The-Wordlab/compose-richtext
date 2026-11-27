package com.halilibo.richtext.markdown

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit

/**
 * Configuration for LaTeX rendering via RaTeX.
 *
 * Formula sizes accept sp or em relative to the surrounding rich text. Unspecified
 * sizes keep the original inline (14 dp) and display (18 dp) sizes.
 *
 * ```kotlin
 * CompositionLocalProvider(
 *   LocalLatexConfig provides LatexConfig(
 *     textColor = MaterialTheme.colorScheme.onBackground,
 *   )
 * ) {
 *   RichText { Markdown(content, markdownParseOptions = CommonMarkdownParseOptions.LatexEnabled) }
 * }
 * ```
 */
@Immutable
public data class LatexConfig(
  val textColor: Color = Color.Black,
  val inlineFontSize: TextUnit = TextUnit.Unspecified,
  val displayFontSize: TextUnit = TextUnit.Unspecified,
) {
  @Deprecated("Retained for binary compatibility", level = DeprecationLevel.HIDDEN)
  public constructor(textColor: Color = Color.Black) :
    this(textColor, TextUnit.Unspecified, TextUnit.Unspecified)

  @Deprecated("Retained for binary compatibility", level = DeprecationLevel.HIDDEN)
  public fun copy(textColor: Color = this.textColor): LatexConfig =
    copy(textColor, inlineFontSize, displayFontSize)
}

/**
 * CompositionLocal to provide [LatexConfig] for LaTeX blocks rendered via RaTeX.
 * Set this from your theme to ensure LaTeX rendering matches your app's color scheme.
 */
public val LocalLatexConfig: ProvidableCompositionLocal<LatexConfig> =
  compositionLocalOf { LatexConfig() }
