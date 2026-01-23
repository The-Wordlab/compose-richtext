package com.halilibo.richtext.commonmark.math

import org.commonmark.Extension
import org.commonmark.parser.Parser

/**
 * CommonMark extension for LaTeX math expressions.
 *
 * Registers:
 * - [MathDelimiterProcessor] for `$...$` (inline) and `$$...$$` (display) math
 * - [MathBlockParser] for multi-line `$$` block math
 *
 * Usage:
 * ```kotlin
 * val parser = Parser.builder()
 *     .extensions(listOf(MathExtension.create()))
 *     .build()
 * ```
 */
public class MathExtension private constructor() : Parser.ParserExtension {

  override fun extend(parserBuilder: Parser.Builder) {
    parserBuilder.customDelimiterProcessor(MathDelimiterProcessor())
    parserBuilder.customBlockParserFactory(MathBlockParser.Factory())
  }

  public companion object {
    public fun create(): Extension = MathExtension()
  }
}
