package com.halilibo.richtext.commonmark.math

import org.commonmark.node.Node
import org.commonmark.node.Text
import org.commonmark.parser.delimiter.DelimiterProcessor
import org.commonmark.parser.delimiter.DelimiterRun

/**
 * CommonMark delimiter processor for `$` character.
 *
 * Handles:
 * - `$...$` → [MathInlineNode] with displayMode=false (inline math)
 * - `$$...$$` → [MathInlineNode] with displayMode=true (display math in inline context)
 *
 * Always creates [MathInlineNode] (a CustomNode/inline node), never [MathDisplayBlock],
 * because delimiter processing happens inside paragraphs (inline context) where block
 * nodes are not allowed.
 *
 * CommonMark's delimiter processing automatically handles:
 * - Content inside code spans (backticks) is not processed
 * - Proper nesting with other delimiters (emphasis, etc.)
 */
internal class MathDelimiterProcessor : DelimiterProcessor {

  override fun getOpeningCharacter(): Char = '$'

  override fun getClosingCharacter(): Char = '$'

  override fun getMinLength(): Int = 1

  override fun process(openingRun: DelimiterRun, closingRun: DelimiterRun): Int {
    // Check for display math ($$...$$) first
    if (openingRun.length() >= 2 && closingRun.length() >= 2) {
      val mathNode = MathInlineNode().apply {
        literal = extractTextBetween(openingRun.opener, closingRun.closer)
        displayMode = true
      }
      moveNodesBetween(openingRun.opener, closingRun.closer, mathNode)
      openingRun.opener.insertAfter(mathNode)
      return 2
    }

    // Single-dollar inline math ($...$)
    if (openingRun.length() >= 1 && closingRun.length() >= 1) {
      val mathNode = MathInlineNode().apply {
        literal = extractTextBetween(openingRun.opener, closingRun.closer)
        displayMode = false
      }
      moveNodesBetween(openingRun.opener, closingRun.closer, mathNode)
      openingRun.opener.insertAfter(mathNode)
      return 1
    }

    return 0
  }

  /**
   * Recursively extracts literal text from all nodes between opener and closer.
   * Handles Text, Emphasis, StrongEmphasis, and any other node types that
   * CommonMark may have parsed within the math delimiters.
   */
  private fun extractTextBetween(opener: Text, closer: Text): String {
    val sb = StringBuilder()
    var node: Node? = opener.next
    while (node != null && node !== closer) {
      extractNodeText(node, sb)
      node = node.next
    }
    return sb.toString()
  }

  private fun extractNodeText(node: Node, sb: StringBuilder) {
    if (node is Text) {
      sb.append(node.literal)
      return
    }
    // For any other node type (Emphasis, StrongEmphasis, Link, etc.),
    // recursively extract text from children
    var child: Node? = node.firstChild
    while (child != null) {
      extractNodeText(child, sb)
      child = child.next
    }
  }

  private fun moveNodesBetween(opener: Text, closer: Text, target: Node) {
    var node: Node? = opener.next
    while (node != null && node !== closer) {
      val next = node.next
      node.unlink()
      target.appendChild(node)
      node = next
    }
  }
}
