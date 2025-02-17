package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.Record
import okio.internal.commonAsUtf8ToByteArray
import kotlin.jvm.JvmStatic

object RecordWeigher {
  private const val SIZE_OF_BOOLEAN = 16
  private const val SIZE_OF_INT = 4
  private const val SIZE_OF_LONG = 8
  private const val SIZE_OF_DOUBLE = 8
  private const val SIZE_OF_ARRAY_OVERHEAD = 16
  private const val SIZE_OF_MAP_OVERHEAD = 16
  private const val SIZE_OF_RECORD_OVERHEAD = 16
  private const val SIZE_OF_CACHE_KEY_OVERHEAD = 16
  private const val SIZE_OF_NULL = 4

  @JvmStatic
  fun byteChange(newValue: Any?, oldValue: Any?): Int {
    return weighField(newValue) - weighField(oldValue)
  }

  @JvmStatic
  fun calculateBytes(record: Record): Int {
    var size = SIZE_OF_RECORD_OVERHEAD + record.key.commonAsUtf8ToByteArray().size
    for ((key, value) in record.fields) {
      size += key.commonAsUtf8ToByteArray().size + weighField(value)
    }
    return size
  }

  private fun weighField(field: Any?): Int {
    return when (field) {
      null -> SIZE_OF_NULL
      is String -> field.commonAsUtf8ToByteArray().size
      is Boolean -> SIZE_OF_BOOLEAN
      is Int -> SIZE_OF_INT
      is Long -> SIZE_OF_LONG // Might happen with LongAdapter
      is Double -> SIZE_OF_DOUBLE
      is List<*> -> {
        SIZE_OF_ARRAY_OVERHEAD + field.sumOf { weighField(it) }
      }
      is CacheKey -> {
        SIZE_OF_CACHE_KEY_OVERHEAD + field.key.commonAsUtf8ToByteArray().size
      }
      /**
       * Custom scalars with a json object representation are stored directly in the record
       */
      is Map<*, *> -> {
        SIZE_OF_MAP_OVERHEAD + field.keys.sumOf { weighField(it) } + field.values.sumOf { weighField(it) }
      }
      else -> error("Unknown field type in Record: '$field'")
    }
  }
}
