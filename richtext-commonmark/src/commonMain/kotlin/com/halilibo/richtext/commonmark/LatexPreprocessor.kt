package com.halilibo.richtext.commonmark

/**
 * Normalizes LaTeX delimiter syntax by converting backslash-paren/bracket notation
 * to dollar notation, which the [MathExtension] delimiter processor handles natively.
 *
 * This is needed because CommonMark's parser interprets `\(` as an escaped `(` character
 * before our extension can see it. By converting to `$...$` / `$$...$$` first, the
 * [MathDelimiterProcessor] handles everything correctly.
 *
 * This is NOT the old full preprocessor — it only converts delimiter syntax and does NOT
 * wrap content in HTML tags or manipulate document structure.
 *
 * Conversions:
 * - `\(...\)` → `$...$`
 * - `\[...\]` → `$$...$$`
 *
 * @param content The markdown content that may contain backslash-notation LaTeX
 * @return Content with normalized delimiters
 */
internal fun normalizeLatexDelimiters(content: String): String {
  var result = content

  // Convert \(...\) to $...$
  result = result.replace(INLINE_PAREN_REGEX) { match ->
    "\$${match.groupValues[1]}\$"
  }

  // Convert \[...\] to $$...$$
  result = result.replace(BLOCK_BRACKET_REGEX) { match ->
    "\$\$${match.groupValues[1]}\$\$"
  }

  return result
}

private val INLINE_PAREN_REGEX = Regex("""\\\((.+?)\\\)""")
private val BLOCK_BRACKET_REGEX = Regex("""\\\[(.+?)\\\]""", RegexOption.DOT_MATCHES_ALL)
