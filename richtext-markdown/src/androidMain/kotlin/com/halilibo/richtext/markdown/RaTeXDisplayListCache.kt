package com.halilibo.richtext.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import io.ratex.DisplayList
import io.ratex.RaTeXEngine

/**
 * LRU cache for parsed [DisplayList] objects. Replaces the heavy [WebView] pool
 * with a lightweight data structure — [DisplayList] is immutable and trivially cacheable.
 */
public class RaTeXDisplayListCache(private val maxSize: Int = DEFAULT_MAX_SIZE) {

  private val cache = object : LinkedHashMap<String, DisplayList>(maxSize + 1, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DisplayList>): Boolean {
      return size > maxSize
    }
  }

  internal suspend fun getOrParse(latex: String, displayMode: Boolean): DisplayList {
    val key = "$displayMode:$latex"
    synchronized(cache) {
      cache[key]?.let { return it }
    }
    val result = RaTeXEngine.parse(latex, displayMode)
    synchronized(cache) {
      cache[key] = result
    }
    return result
  }

  public fun clear() {
    synchronized(cache) {
      cache.clear()
    }
  }

  public companion object {
    public const val DEFAULT_MAX_SIZE: Int = 50
  }
}

public val LocalRaTeXDisplayListCache: ProvidableCompositionLocal<RaTeXDisplayListCache?> =
  compositionLocalOf { null }

@Composable
public fun rememberRaTeXDisplayListCache(
  maxSize: Int = RaTeXDisplayListCache.DEFAULT_MAX_SIZE
): RaTeXDisplayListCache {
  val cache = remember { RaTeXDisplayListCache(maxSize) }
  DisposableEffect(cache) {
    onDispose { cache.clear() }
  }
  return cache
}
