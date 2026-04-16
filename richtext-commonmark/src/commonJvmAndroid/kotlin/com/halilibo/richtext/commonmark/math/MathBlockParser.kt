package com.halilibo.richtext.commonmark.math

import org.commonmark.node.Block
import org.commonmark.parser.SourceLine
import org.commonmark.parser.block.AbstractBlockParser
import org.commonmark.parser.block.AbstractBlockParserFactory
import org.commonmark.parser.block.BlockContinue
import org.commonmark.parser.block.BlockStart
import org.commonmark.parser.block.MatchedBlockParser
import org.commonmark.parser.block.ParserState

/**
 * Block-level parser for display math delimited by `$$` on separate lines:
 * ```
 * $$
 * \int_0^1 x^2 dx = \frac{1}{3}
 * $$
 * ```
 */
internal class MathBlockParser : AbstractBlockParser() {

  private val block = MathDisplayBlock()
  private val content = StringBuilder()

  override fun getBlock(): Block = block

  override fun tryContinue(state: ParserState): BlockContinue? {
    val line = state.line.content.toString().trim()
    return if (line == "$$") {
      BlockContinue.finished()
    } else {
      BlockContinue.atIndex(state.index)
    }
  }

  override fun addLine(line: SourceLine) {
    if (content.isNotEmpty()) {
      content.append('\n')
    }
    content.append(line.content)
  }

  override fun closeBlock() {
    block.literal = content.toString().trim()
  }

  class Factory : AbstractBlockParserFactory() {
    override fun tryStart(
      state: ParserState,
      matchedBlockParser: MatchedBlockParser
    ): BlockStart? {
      val indent = state.indent
      if (indent >= 4) return null

      val line = state.line.content.toString()
      val contentAfterIndent = line.substring(state.index)

      // Must start with $$ and have nothing else meaningful on the line
      if (contentAfterIndent.trimEnd() == "$$") {
        return BlockStart.of(MathBlockParser()).atIndex(state.line.content.length)
      }

      return null
    }
  }
}
