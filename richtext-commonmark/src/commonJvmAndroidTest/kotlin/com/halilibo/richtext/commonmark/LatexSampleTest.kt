package com.halilibo.richtext.commonmark

import com.halilibo.richtext.markdown.node.AstDisplayMath
import com.halilibo.richtext.markdown.node.AstCode
import com.halilibo.richtext.markdown.node.AstFencedCodeBlock
import com.halilibo.richtext.markdown.node.AstIndentedCodeBlock
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

    @Test
    fun `space padded inline dollar command becomes math`() {
        val ast = parser.parse("Inline: \$ \\frac{9}{2} \$")
        val math = ast.collectTypes().filterIsInstance<AstInlineMath>()
        assertEquals(1, math.size)
        assertEquals("\\frac{9}{2}", math.single().literal)
    }

    @Test
    fun `space padded display dollar command becomes display math`() {
        val ast = parser.parse("Display: \$\$ \\boxed{\\frac{9}{2}} \$\$")
        val math = ast.collectTypes().filterIsInstance<AstInlineMath>()
        assertEquals(1, math.size)
        assertTrue(math.single().displayMode)
        assertEquals("\\boxed{\\frac{9}{2}}", math.single().literal)
    }

    @Test
    fun `multiline bracket cases reach the renderer as a single LaTeX line`() {
        val markdown = """
            \[
            \begin{cases}
            a) \frac{7}{2} \\
            b) \frac{11}{9}
            \end{cases}
            \]
        """.trimIndent()
        val ast = parser.parse(normalizeLatexDelimiters(markdown))
        val math = ast.collectTypes().filterIsInstance<AstDisplayMath>()
        assertEquals(1, math.size)
        assertEquals("\\begin{cases} a) \\frac{7}{2} \\\\ b) \\frac{11}{9} \\end{cases}", math.single().literal)
    }

    @Test
    fun `multiline bracket array reaches the renderer as a single LaTeX line`() {
        val markdown = """
            \[
            \begin{array}{r|l}
            A & \frac{9}{2} \\
            B & \frac{11}{9}
            \end{array}
            \]
        """.trimIndent()
        val ast = parser.parse(normalizeLatexDelimiters(markdown))
        val math = ast.collectTypes().filterIsInstance<AstDisplayMath>()
        assertEquals(1, math.size)
        assertEquals("\\begin{array}{r|l} A & \\frac{9}{2} \\\\ B & \\frac{11}{9} \\end{array}", math.single().literal)
    }

    @Test
    fun `fenced PostgreSQL dollar block keeps line breaks and is not math`() {
        val markdown = """
            ```sql
            DO ${'$'}${'$'}
            -- a SQL comment
            SELECT 1;
            ${'$'}${'$'};
            ```
        """.trimIndent()
        val ast = parser.parse(markdown)
        val nodes = ast.collectTypes()
        assertTrue(nodes.filterIsInstance<AstInlineMath>().isEmpty())
        assertTrue(nodes.filterIsInstance<AstDisplayMath>().isEmpty())
        val code = nodes.filterIsInstance<AstFencedCodeBlock>().single().literal
        assertTrue(code.contains("DO ${'$'}${'$'}\n-- a SQL comment\nSELECT 1;\n${'$'}${'$'};"))
    }

    @Test
    fun `indented and inline code containing padded math remain code`() {
        val markdown = """
            `${'$'} \frac{9}{2} ${'$'}`

                DO ${'$'}${'$'}
                SELECT 1;
                ${'$'}${'$'};
        """.trimIndent()
        val nodes = parser.parse(markdown).collectTypes()
        assertTrue(nodes.filterIsInstance<AstInlineMath>().isEmpty())
        assertTrue(nodes.filterIsInstance<AstDisplayMath>().isEmpty())
        assertEquals("${'$'} \\frac{9}{2} ${'$'}", nodes.filterIsInstance<AstCode>().single().literal)
        assertTrue(nodes.filterIsInstance<AstIndentedCodeBlock>().single().literal.contains("DO ${'$'}${'$'}\nSELECT 1;"))
    }

    @Test
    fun `currency and escaped dollar remain prose`() {
        val currency = parser.parse("The price moved from ${'$'} 5 to ${'$'} 10.")
        val escaped = parser.parse("Literal: \\${'$'} \\frac{9}{2} ${'$'}")
        assertTrue(currency.collectTypes().filterIsInstance<AstInlineMath>().isEmpty())
        assertTrue(escaped.collectTypes().filterIsInstance<AstInlineMath>().isEmpty())
    }

    @Test
    fun `an escaped dollar does not hide later real math`() {
        val ast = parser.parse("Literal: \\${'$'} \\frac{9}{2} ${'$'}; real: ${'$'} \\frac{11}{9} ${'$'}")
        val math = ast.collectTypes().filterIsInstance<AstInlineMath>()
        assertEquals(1, math.size)
        assertEquals("\\frac{11}{9}", math.single().literal)
    }

    @Test
    fun `two padded commands in one sentence both become math`() {
        val ast = parser.parse("First ${'$'} \\frac{9}{2} ${'$'} and second ${'$'} \\frac{11}{9} ${'$'}.")
        val math = ast.collectTypes().filterIsInstance<AstInlineMath>()
        assertEquals(listOf("\\frac{9}{2}", "\\frac{11}{9}"), math.map { it.literal })
    }
}
