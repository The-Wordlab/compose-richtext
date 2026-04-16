package com.halilibo.richtext.markdown

import androidx.compose.runtime.Composable

@Composable
internal actual fun DisplayMathContent(latex: String) {
  val config = LocalLatexConfig.current
  RaTeXBlockFormula(latex = latex, textColor = config.textColor)
}
