package com.halilibo.richtext.commonmark.math

import org.commonmark.node.Node
import org.commonmark.node.Text

private val displayDollarMath = Regex("""(?<!\$)\$\$(?!\$)([^$\r\n]*?)\$\$(?!\$)""")
private val inlineDollarMath = Regex("""(?<!\$)\$(?!\$)([^$\r\n]*?)\$(?!\$)""")
private val latexLineBreak = Regex("[\\t ]*\\r?\\n[\\t ]*")

/** Normalizes only parsed math or prose text nodes, never code-block or code-span literals. */
internal fun normalizeParsedMath(root: Node, markdown: String) {
  val sourceLines = markdown.split('\n')

  fun visit(node: Node?) {
    var current = node
    while (current != null) {
      val next = current.next
      when (current) {
        is MathDisplayBlock -> current.literal = current.literal.normalizeLatexLines()
        is MathInlineNode -> current.literal = current.literal.normalizeLatexLines()
        is Text -> current.replaceSpacedDollarMath(sourceLines)
      }
      visit(current.firstChild)
      current = next
    }
  }

  visit(root)
}

private fun String.normalizeLatexLines(): String = trim().replace(latexLineBreak, " ")

private fun Text.replaceSpacedDollarMath(sourceLines: List<String>) {
  val original = literal
  val rawSource = sourceSpans.mapNotNull { span ->
    sourceLines.getOrNull(span.lineIndex)?.let { line ->
      val start = span.columnIndex.coerceAtMost(line.length)
      val end = (start + span.length).coerceAtMost(line.length)
      line.substring(start, end)
    }
  }.joinToString("")
  val rawMatches = (displayDollarMath.findAll(rawSource) + inlineDollarMath.findAll(rawSource))
    .sortedBy { it.range.first }
    .toList()
  var matchIndex = 0
  var searchFrom = 0
  var emittedThrough = 0

  while (searchFrom < original.length) {
    val display = displayDollarMath.find(original, searchFrom)
    val inline = inlineDollarMath.find(original, searchFrom)
    val match = when {
      display == null -> inline
      inline == null -> display
      display.range.first <= inline.range.first -> display
      else -> inline
    } ?: break

    searchFrom = match.range.last + 1
    val rawMatch = rawMatches.getOrNull(matchIndex++) ?: continue
    val latex = match.groupValues[1]
    if (rawMatch.groupValues[1] != latex || rawSource.isEscapedAt(rawMatch.range.first) ||
      latex == latex.trim() || '\\' !in latex || latex.isBlank()
    ) continue

    if (match.range.first > emittedThrough) {
      insertBefore(Text(original.substring(emittedThrough, match.range.first)))
    }
    insertBefore(MathInlineNode().apply {
      literal = latex.trim()
      displayMode = match.groupValues[0].startsWith("\$\$")
    })
    emittedThrough = searchFrom
  }

  if (emittedThrough > 0) {
    if (emittedThrough < original.length) insertBefore(Text(original.substring(emittedThrough)))
    unlink()
  }
}

private fun String.isEscapedAt(index: Int): Boolean {
  var slashCount = 0
  var previous = index - 1
  while (previous >= 0 && this[previous] == '\\') {
    slashCount++
    previous--
  }
  return slashCount % 2 == 1
}
