package com.halilibo.richtext.commonmark.math

import org.commonmark.node.CustomBlock

/**
 * CommonMark custom block node for display math expressions.
 * Created by [MathBlockParser] for multi-line `$$...\n...\n$$` blocks,
 * and by [MathDelimiterProcessor] for inline `$$...$$` display math.
 */
internal class MathDisplayBlock : CustomBlock() {
  var literal: String = ""
}
