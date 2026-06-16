package com.halilibo.richtext.commonmark

import kotlin.test.Test
import kotlin.test.assertEquals

class LatexDelimiterNormalizerTest {

    @Test
    fun `normalizeLatexDelimiters should convert backslash-paren to dollar`() {
        val input = "Inline: \\(a + b = c\\) math"
        val result = normalizeLatexDelimiters(input)
        assertEquals("Inline: \$a + b = c\$ math", result)
    }

    @Test
    fun `normalizeLatexDelimiters should convert backslash-bracket to double dollar`() {
        val input = "Block: \\[\\sum_{i=1}^n i\\]"
        val result = normalizeLatexDelimiters(input)
        assertEquals("Block: \$\$\\sum_{i=1}^n i\$\$", result)
    }

    @Test
    fun `normalizeLatexDelimiters should not modify text without backslash notation`() {
        val input = "Plain text with \$x^2\$ and \$\$y^2\$\$"
        val result = normalizeLatexDelimiters(input)
        assertEquals(input, result)
    }

    @Test
    fun `normalizeLatexDelimiters should handle mixed notation`() {
        val input = "Inline \\(x\\) and dollar \$y\$ and block \\[z\\]"
        val result = normalizeLatexDelimiters(input)
        assertEquals("Inline \$x\$ and dollar \$y\$ and block \$\$z\$\$", result)
    }

    @Test
    fun `normalizeLatexDelimiters should trim spaces padding inline delimiters`() {
        // Backends commonly pad delimiters with spaces. Without trimming the result would be
        // "$ f = ... Hz $", which violates CommonMark's flanking rules and leaks literal dollars.
        val input = "Jawab: \\( f = \\frac{1}{0,25} = 4 \\text{ Hz} \\)"
        val result = normalizeLatexDelimiters(input)
        assertEquals("Jawab: \$f = \\frac{1}{0,25} = 4 \\text{ Hz}\$", result)
    }

    @Test
    fun `normalizeLatexDelimiters should trim spaces padding block delimiters`() {
        val input = "Block \\[ \\sum_{i=1}^n i \\]"
        val result = normalizeLatexDelimiters(input)
        assertEquals("Block \$\$\\sum_{i=1}^n i\$\$", result)
    }

    @Test
    fun `normalizeLatexDelimiters should trim multiline block delimiters`() {
        val input = "Rumus:\n\\[\n  f = \\frac{n}{t}\n\\]"
        val result = normalizeLatexDelimiters(input)
        assertEquals("Rumus:\n\$\$f = \\frac{n}{t}\$\$", result)
    }
}
