package com.halilibo.richtext.markdown

import androidx.compose.runtime.Composable

@Composable
internal actual fun InlineMathContent(latex: String) {
  val config = LocalLatexConfig.current
  RaTeXInlineFormula(latex = latex, textColor = config.textColor)
}
