package com.halilibo.richtext.markdown

import androidx.compose.runtime.Composable

/**
 * Platform-specific display/block math rendering. On Android, renders via RaTeX.
 * On Desktop/JVM, falls back to displaying the raw LaTeX as text.
 */
@Composable
internal expect fun DisplayMathContent(latex: String)
