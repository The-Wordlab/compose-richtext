package com.halilibo.richtext.markdown

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Configuration for LaTeX rendering via RaTeX.
 *
 * Provide via [LocalLatexConfig] to customize the text color for rendered formulas.
 * When [textColor] is [Color.Unspecified], the renderer falls back to the current
 * [LocalContentColor].
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
)

/**
 * CompositionLocal to provide [LatexConfig] for LaTeX blocks rendered via RaTeX.
 * Set this from your theme to ensure LaTeX rendering matches your app's color scheme.
 */
public val LocalLatexConfig: ProvidableCompositionLocal<LatexConfig> =
  compositionLocalOf { LatexConfig() }
