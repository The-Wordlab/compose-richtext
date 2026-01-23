package com.halilibo.richtext.commonmark

/**
 * Allows configuration of the Markdown parser
 *
 * @param autolink Detect plain text links and turn them into Markdown links.
 * @param enableLatex Enable LaTeX math expression preprocessing. When true, inline ($...$, \(...\))
 * and display ($$...$$, \[...\]) math expressions are wrapped in HTML blocks for MathJax rendering.
 */
public class CommonMarkdownParseOptions(
  public val autolink: Boolean,
  public val enableLatex: Boolean = false
) {

  override fun toString(): String {
    return "CommonMarkdownParseOptions(autolink=$autolink, enableLatex=$enableLatex)"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CommonMarkdownParseOptions) return false

    if (autolink != other.autolink) return false
    if (enableLatex != other.enableLatex) return false
    return true
  }

  override fun hashCode(): Int {
    var result = autolink.hashCode()
    result = 31 * result + enableLatex.hashCode()
    return result
  }

  public fun copy(
    autolink: Boolean = this.autolink,
    enableLatex: Boolean = this.enableLatex
  ): CommonMarkdownParseOptions = CommonMarkdownParseOptions(
    autolink = autolink,
    enableLatex = enableLatex
  )

  public companion object {
    public val Default: CommonMarkdownParseOptions = CommonMarkdownParseOptions(
      autolink = true
    )

    public val LatexEnabled: CommonMarkdownParseOptions = CommonMarkdownParseOptions(
      autolink = true,
      enableLatex = true
    )
  }
}
