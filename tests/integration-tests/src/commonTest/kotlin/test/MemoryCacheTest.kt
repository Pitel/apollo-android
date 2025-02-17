package test

import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.MemoryCache
import com.apollographql.apollo3.cache.normalized.Record
import com.apollographql.apollo3.testing.runWithMainLoop
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MemoryCacheTest {
  @Test
  fun testDoesNotExpireBeforeMillis() = runWithMainLoop {
    val record =  Record(
        key = "key",
        fields = mapOf(
            "field" to "value"
        )
    )
    val memoryCache = MemoryCache(expireAfterMillis = 200)
    memoryCache.merge(record, CacheHeaders.NONE)

    val cacheRecord = checkNotNull(memoryCache.loadRecord(record.key, CacheHeaders.NONE))
    assertEquals(record.key, cacheRecord.key)
    assertEquals(record.fields, cacheRecord.fields)

    delay(250)
    assertNull(memoryCache.loadRecord(record.key, CacheHeaders.NONE))
  }
}