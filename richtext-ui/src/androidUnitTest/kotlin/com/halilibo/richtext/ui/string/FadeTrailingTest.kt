package com.halilibo.richtext.ui.string

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import com.halilibo.richtext.ui.RichTextFadeIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FadeTrailingTest {
  private val fadeIn = RichTextFadeIn(characters = 36, minAlpha = 0.05f)

  @Test
  fun `GIVEN plain text WHEN fading the tail THEN every character gets its own step of the ramp`() {
    val faded = AnnotatedString("Hello").fade()

    assertEquals(listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5), faded.spanRanges())
    assertEquals(fadeIn.minAlpha, faded.alphaAt(4), ALPHA_TOLERANCE)
    assertTrue(faded.alphaAt(0) > faded.alphaAt(4))
  }

  @Test
  fun `GIVEN an emoji in the tail WHEN fading THEN no span cuts between its two halves`() {
    val text = "Hi Farhad! 😊 How's your day going?"

    val faded = AnnotatedString(text).fade()

    val emojiStart = text.indexOf('\uD83D')
    assertTrue(faded.spanRanges().contains(emojiStart to emojiStart + 2))
    assertNoSpanSplitsASurrogatePair(faded)
  }

  @Test
  fun `GIVEN an emoji followed by more text WHEN fading THEN the ramp keeps its distance from the end`() {
    val withEmoji = AnnotatedString("a😊bc").fade()
    val plain = AnnotatedString("a__bc").fade()

    assertEquals(plain.alphaAt(4), withEmoji.alphaAt(4), ALPHA_TOLERANCE)
    assertEquals(plain.alphaAt(2), withEmoji.alphaAt(1), ALPHA_TOLERANCE)
    assertEquals(plain.alphaAt(0), withEmoji.alphaAt(0), ALPHA_TOLERANCE)
  }

  @Test
  fun `GIVEN a joined family emoji WHEN fading THEN the whole sequence shares one span`() {
    val family = "👨‍👩‍👧"

    val faded = AnnotatedString("a${family}b").fade()

    assertEquals(listOf(0 to 1, 1 to 1 + family.length, 1 + family.length to 2 + family.length), faded.spanRanges())
  }

  @Test
  fun `GIVEN a variation selector, skin tone, keycap or flag WHEN fading THEN each stays in one span`() {
    val heart = "❤️"
    val wave = "👋🏽"
    val keycap = "1️⃣"
    val flag = "🇪🇸"

    listOf(heart, wave, keycap, flag).forEach { cluster ->
      val faded = AnnotatedString("a${cluster}b").fade()

      assertEquals(listOf(0 to 1, 1 to 1 + cluster.length, 1 + cluster.length to 2 + cluster.length), faded.spanRanges())
    }
  }

  @Test
  fun `GIVEN an emoji straddling the start of the ramp WHEN fading THEN it is not cut there either`() {
    val text = "😊" + "x".repeat(fadeIn.characters - 1)

    val faded = AnnotatedString(text).fade()

    assertNoSpanSplitsASurrogatePair(faded)
  }

  @Test
  fun `GIVEN a block beyond the ramp WHEN fading THEN the text is returned untouched`() {
    val original = AnnotatedString("Hello 😊")

    val faded = original.fadeTrailing(fadeIn, tailOffset = fadeIn.characters, color = Color.White)

    assertTrue(faded.spanStyles.isEmpty())
  }

  private fun AnnotatedString.fade(): AnnotatedString =
    fadeTrailing(fadeIn, tailOffset = 0, color = Color.White)

  private fun AnnotatedString.spanRanges(): List<Pair<Int, Int>> =
    spanStyles.map { it.start to it.end }.sortedBy { it.first }

  private fun AnnotatedString.alphaAt(index: Int): Float =
    spanStyles.first { index >= it.start && index < it.end }.item.color.alpha

  private fun assertNoSpanSplitsASurrogatePair(faded: AnnotatedString) {
    val edges = faded.spanStyles.flatMap { listOf(it.start, it.end) }.filter { it in 1 until faded.length }
    edges.forEach { edge ->
      val splits = faded.text[edge - 1].isHighSurrogate() && faded.text[edge].isLowSurrogate()
      assertTrue("span edge at $edge cuts an emoji in half", !splits)
    }
  }

  private companion object {
    const val ALPHA_TOLERANCE = 0.005f
  }
}
