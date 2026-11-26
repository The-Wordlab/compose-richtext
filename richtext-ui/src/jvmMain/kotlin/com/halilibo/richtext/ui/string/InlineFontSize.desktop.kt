package com.halilibo.richtext.ui.string

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit

internal actual fun Density.absoluteSpanFontSizePx(size: TextUnit): Float = size.toPx()
