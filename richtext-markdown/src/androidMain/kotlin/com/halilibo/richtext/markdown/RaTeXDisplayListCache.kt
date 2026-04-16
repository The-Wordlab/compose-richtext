package com.halilibo.richtext.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

/**
 * LRU cache for pre-rendered LaTeX formula bitmaps.
 * Caches the final colored bitmap so that scrolling in a LazyColumn
 * never needs to re-parse or re-render — just a single drawBitmap() call.
 *
 * Key: (latex, displayMode, fontSizePx, colorArgb)
 */
public class RaTeXBitmapCache(private val maxSize: Int = DEFAULT_MAX_SIZE) {

  private val cache = object : LinkedHashMap<String, BitmapResult>(maxSize + 1, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BitmapResult>): Boolean {
      if (size > maxSize) {
        eldest.value.bitmap.recycle()
        return true
      }
      return false
    }
  }

  private fun key(latex: String, displayMode: Boolean, fontSizePx: Float, colorArgb: Int): String {
    return "$displayMode:$fontSizePx:$colorArgb:$latex"
  }

  internal fun get(latex: String, displayMode: Boolean, fontSizePx: Float, colorArgb: Int): BitmapResult? {
    synchronized(cache) {
      return cache[key(latex, displayMode, fontSizePx, colorArgb)]
    }
  }

  internal fun put(latex: String, displayMode: Boolean, fontSizePx: Float, colorArgb: Int, result: BitmapResult) {
    synchronized(cache) {
      val k = key(latex, displayMode, fontSizePx, colorArgb)
      if (!cache.containsKey(k)) {
        cache[k] = result
      }
    }
  }

  public fun clear() {
    synchronized(cache) {
      cache.values.forEach { it.bitmap.recycle() }
      cache.clear()
    }
  }

  public companion object {
    public const val DEFAULT_MAX_SIZE: Int = 100
  }
}

public val LocalRaTeXBitmapCache: ProvidableCompositionLocal<RaTeXBitmapCache?> =
  compositionLocalOf { null }

@Composable
public fun rememberRaTeXBitmapCache(
  maxSize: Int = RaTeXBitmapCache.DEFAULT_MAX_SIZE
): RaTeXBitmapCache {
  val cache = remember { RaTeXBitmapCache(maxSize) }
  DisposableEffect(cache) {
    onDispose { cache.clear() }
  }
  return cache
}
