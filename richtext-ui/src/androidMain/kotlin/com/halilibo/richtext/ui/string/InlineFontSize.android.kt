package com.halilibo.richtext.ui.string

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit

// Android's AbsoluteSizeSpan stores whole physical pixels.
internal actual fun Density.absoluteSpanFontSizePx(size: TextUnit): Float = size.roundToPx().toFloat()
