package com.halilibo.richtext.markdown

import androidx.compose.runtime.Composable

@Composable
internal actual fun InlineMathContent(latex: String) {
  RaTeXFormula(latex, displayMode = false)
}
