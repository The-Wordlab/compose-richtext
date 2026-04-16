package com.zachklipp.richtext.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.commonmark.CommonMarkdownParseOptions
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.markdown.LatexConfig
import com.halilibo.richtext.markdown.LocalLatexConfig
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.ui.resolveDefaults

@Preview
@Composable private fun LatexSamplePreview() {
    LatexSample()
}

@Composable fun LatexSample() {
    var richTextStyle by remember { mutableStateOf(RichTextStyle().resolveDefaults()) }
    val isDarkModeEnabled = true
    val colors = if (isDarkModeEnabled) darkColorScheme() else androidx.compose.material3.lightColorScheme()
    val context = LocalContext.current

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr
    ) {
        SampleTheme(colorScheme = colors) {
            Surface {
                CompositionLocalProvider(
                    LocalLatexConfig provides LatexConfig(
                        textColor = MaterialTheme.colorScheme.onBackground
                    )
                ) {
                    SelectionContainer {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            ProvideToastUriHandler(context) {
                                RichText(
                                    style = richTextStyle,
                                    modifier = Modifier.padding(16.dp),
                                ) {
                                    Markdown(
                                        content = sampleLatexMarkdown,
                                        markdownParseOptions = CommonMarkdownParseOptions.LatexEnabled
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val sampleLatexMarkdown = """
# LaTeX Support Demo

## Math message
Hello! Let's solve the quadratic equation: ${'$'}${'$'}x^2 + 10x + 16 = 0${'$'}${'$'}

1. Identify the coefficients: \(a = 1\), \(b = 10\), and \(c = 16\).

2. Calculate the discriminant \(\Delta = b^2 - 4ac\):
   ${'$'}${'$'}
   \Delta = 10^2 - 4 \cdot 1 \cdot 16 = 100 - 64 = 36
   ${'$'}${'$'}

3. Since \(\Delta > 0\), there are two distinct real roots. Use the quadratic formula:
   ${'$'}${'$'}
   x = \frac{-b \pm \sqrt{\Delta}}{2a} = \frac{-10 \pm \sqrt{36}}{2 \cdot 1} = \frac{-10 \pm 6}{2}
   ${'$'}${'$'}

4. Calculate each root:
   - For the plus sign:
     ${'$'}${'$'}
     x_1 = \frac{-10 + 6}{2} = \frac{-4}{2} = -2
     ${'$'}${'$'}
   - For the minus sign:
     ${'$'}${'$'}
     x_2 = \frac{-10 - 6}{2} = \frac{-16}{2} = -8
     ${'$'}${'$'}

😊 **The solutions are \(x = -2\) and \(x = -8\)**

This demonstrates LaTeX rendering support in compose-richtext.

## Inline Math

Here's some inline math: ${'$'}E = mc^2${'$'} which shows Einstein's famous equation.

You can also write inline expressions using LaTeX brackets:
\(a^2 + b^2 = c^2\)

## Display Math (Block)

Display math using double dollars:

$$
\int_{-\infty}^{\infty} e^{-x^2} dx = \sqrt{\pi}
$$

Or using LaTeX square brackets:

\[\\sum_{n=1}^{\infty} \frac{1}{n^2} = \frac{\pi^2}{6}\]

## Complex Expressions

### Calculus
The derivative of ${'$'}\sin(x)${'$'} is ${'$'}\cos(x)${'$'}:

$$
\frac{d}{dx}\sin(x) = \cos(x)
$$

### Linear Algebra
Matrix multiplication:

$$
\begin{pmatrix} a & b \\ c & d \end{pmatrix}
\begin{pmatrix} x \\ y \end{pmatrix}
=
\begin{pmatrix} ax + by \\ cx + dy \end{pmatrix}
$$

### Statistics
The normal distribution probability density function:

$$
f(x) = \frac{1}{\sigma\sqrt{2\pi}}
e^{-\frac{1}{2}\left(\frac{x-\mu}{\sigma}\right)^2}
$$

## Mixed Content

- First item with inline math: ${'$'}\alpha + \beta = \gamma${'$'}
- Second item with display math:

${'$'}${'$'}
\sum_{k=1}^n k^2 = \frac{n(n+1)(2n+1)}{6}
${'$'}${'$'}

## Edge Cases

### Currency (should NOT render as LaTeX)

The price is ${'$'}100. Items cost ${'$'}50 and ${'$'}200 respectively.

### Escaped dollar signs

Use \${'$'} for currency in LaTeX documents.

### LaTeX inside inline code (should NOT render)

Use `${'$'}x^2${'$'}` for inline math and `${'$'}${'$'}...${'$'}${'$'}` for display math.

### LaTeX inside fenced code blocks (should NOT render)

```
${'$'}${'$'}
\int_0^1 x^2 dx = \frac{1}{3}
${'$'}${'$'}
```

## Messages samples

To solve the equation $2x + 3 = 11$:  1.  Subtract 3 from both sides:     $2x = 11 - 3$     $2x = 8$  2.  Divide both sides by 2:     ${'$'}x = 8 / 2$     **${'$'}x = 4$**

""".trimIndent()