package com.halilibo.richtext.markdown

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable

@Composable
internal actual fun DisplayMathContent(latex: String) {
  BasicText(latex)
}
