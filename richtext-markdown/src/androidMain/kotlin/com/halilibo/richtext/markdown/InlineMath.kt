package com.halilibo.richtext.markdown

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal actual fun InlineMathContent(latex: String) {
  val fontSize = LocalLatexConfig.current.inlineFontSize.value
  val modifier = if (fontSize.isFinite() && fontSize > 0f) {
    Modifier.padding(vertical = 2.dp)
  } else {
    Modifier
  }
  RaTeXFormula(latex, displayMode = false, modifier = modifier)
}
