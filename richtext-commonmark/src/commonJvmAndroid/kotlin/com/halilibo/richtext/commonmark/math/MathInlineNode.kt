package com.halilibo.richtext.commonmark.math

import org.commonmark.node.CustomNode

/**
 * CommonMark custom inline node for math expressions.
 * Used for both `$...$` (inline) and `$$...$$` (display) when they appear within a paragraph.
 *
 * @property literal The raw LaTeX content
 * @property displayMode true for `$$...$$` (display/block style), false for `$...$` (inline style)
 */
internal class MathInlineNode : CustomNode() {
  var literal: String = ""
  var displayMode: Boolean = false
}
