package com.halilibo.richtext.commonmark

import com.halilibo.richtext.markdown.node.AstDisplayMath
import com.halilibo.richtext.markdown.node.AstInlineMath
import com.halilibo.richtext.markdown.node.AstNode
import com.halilibo.richtext.markdown.node.AstNodeType
import com.halilibo.richtext.markdown.node.AstParagraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MathExtensionTest {

    private val parser = CommonmarkAstNodeParser(CommonMarkdownParseOptions.LatexEnabled)

    private fun AstNode.collectTypes(): List<AstNodeType> {
        val types = mutableListOf<AstNodeType>()
        fun walk(node: AstNode?) {
            node ?: return
            types.add(node.type)
            walk(node.links.firstChild)
            walk(node.links.next)
        }
        walk(this)
        return types
    }

    @Test
    fun `inline math produces AstInlineMath`() {
        val ast = parser.parse("Here is \$x^2\$ math")
        val inlineMath = ast.collectTypes().filterIsInstance<AstInlineMath>()
        assertEquals(1, inlineMath.size)
        assertEquals("x^2", inlineMath[0].literal)
    }

    @Test
    fun `single character inline math works`() {
        val ast = parser.parse("Solve for \$x\$ in the equation")
        val inlineMath = ast.collectTypes().filterIsInstance<AstInlineMath>()
        assertEquals(1, inlineMath.size)
        assertEquals("x", inlineMath[0].literal)
    }

    @Test
    fun `display math with double dollars on own lines`() {
        val ast = parser.parse("text\n\n\$\$\n\\int_0^1 x^2 dx\n\$\$\n\nmore")
        val displayMath = ast.collectTypes().filterIsInstance<AstDisplayMath>()
        assertEquals(1, displayMath.size)
        assertTrue(displayMath[0].literal.contains("\\int_0^1"))
    }

    @Test
    fun `inline display math with double dollars`() {
        val ast = parser.parse("text \$\$x^2 + y^2\$\$ more")
        // When $$...$$ appears inside a paragraph, it creates AstInlineMath with displayMode=true
        val inlineMath = ast.collectTypes().filterIsInstance<AstInlineMath>()
        val displayMath = inlineMath.filter { it.displayMode }
        assertEquals(1, displayMath.size)
        assertEquals("x^2 + y^2", displayMath[0].literal)
    }

    @Test
    fun `math inside code span is not processed`() {
        val ast = parser.parse("Use `\$x^2\$` for math")
        val inlineMath = ast.collectTypes().filterIsInstance<AstInlineMath>()
        assertEquals(0, inlineMath.size)
    }

    @Test
    fun `math inside fenced code block is not processed`() {
        val ast = parser.parse("```\n\$\$\n\\int_0^1 x dx\n\$\$\n```")
        val displayMath = ast.collectTypes().filterIsInstance<AstDisplayMath>()
        assertEquals(0, displayMath.size)
    }

    @Test
    fun `inline math in list items works`() {
        val ast = parser.parse("1. Item with \$x = 1\$\n2. Item with \$y = 2\$")
        val inlineMath = ast.collectTypes().filterIsInstance<AstInlineMath>()
        assertEquals(2, inlineMath.size)
        assertEquals("x = 1", inlineMath[0].literal)
        assertEquals("y = 2", inlineMath[1].literal)
    }

    @Test
    fun `backslash-paren notation converted to inline math`() {
        val content = normalizeLatexDelimiters("Here \\(a + b\\) works")
        val ast = parser.parse(content)
        val inlineMath = ast.collectTypes().filterIsInstance<AstInlineMath>()
        assertEquals(1, inlineMath.size)
        assertEquals("a + b", inlineMath[0].literal)
    }

    @Test
    fun `sample content processes correctly`() {
        val sampleContent = """
            # LaTeX Support Demo

            Here's inline math: ${'$'}E = mc^2${'$'} and display math:

            ${'$'}${'$'}
            \int_{0}^{1} x dx = \frac{1}{2}
            ${'$'}${'$'}
        """.trimIndent()

        val ast = parser.parse(sampleContent)
        val types = ast.collectTypes()

        val inlineMath = types.filterIsInstance<AstInlineMath>()
        assertNotNull(inlineMath.find { it.literal == "E = mc^2" })

        val displayMath = types.filterIsInstance<AstDisplayMath>()
        assertTrue(displayMath.isNotEmpty())
    }
}
