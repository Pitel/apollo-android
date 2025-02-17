package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.CompiledCondition
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CompiledFragment
import com.apollographql.apollo3.api.CompiledListType
import com.apollographql.apollo3.api.CompiledNamedType
import com.apollographql.apollo3.api.CompiledNotNullType
import com.apollographql.apollo3.api.CompiledSelection
import com.apollographql.apollo3.api.CompiledType
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.isComposite
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.ObjectIdGenerator
import com.apollographql.apollo3.cache.normalized.ObjectIdGeneratorContext
import com.apollographql.apollo3.cache.normalized.Record

/**
 * A [Normalizer] takes a [Map]<String, Any?> and turns them into a flat list of [Record]
 * The key of each [Record] is given by [objectIdGenerator] or defaults to using the path
 */
class Normalizer(
    private val variables: Executable.Variables,
    private val rootKey: String,
    private val objectIdGenerator: ObjectIdGenerator,
) {
  private val records = mutableMapOf<String, Record>()

  fun normalize(map: Map<String, Any?>, selections: List<CompiledSelection>): Map<String, Record> {
    buildRecord(map, rootKey, selections)

    return records
  }

  /**
   * @param obj the json node representing the object
   * @param key the key for this record
   * @param selections the selections queried on this object
   * @return the CacheKey
   */
  private fun buildRecord(
      obj: Map<String, Any?>,
      key: String,
      selections: List<CompiledSelection>,
  ): CacheKey {

    val typename = obj["__typename"] as? String
    val allFields = collectFields(selections, typename)

    val record = Record(
        key = key,
        fields = obj.entries.mapNotNull { entry ->
          val compiledFields = allFields.filter { it.responseName == entry.key }
          if (compiledFields.isEmpty()) {
            // If we come here, `obj` contains more data than the CompiledSelections can understand
            // It's not 100% clear how this could happen, log what field triggered this
            throw RuntimeException("Cannot find a CompiledField for entry: {${entry.key}: ${entry.value}}, __typename = $typename, key = $key")
          }
          val includedFields = compiledFields.filter {
            !it.shouldSkip(variableValues = variables.valueMap)
          }
          if (includedFields.isEmpty()) {
            // If the field is absent, we don't want to serialize "null" to the cache, do not include this field in the record.
            return@mapNotNull null
          }
          val mergedField = includedFields.first().newBuilder()
              .selections(includedFields.flatMap { it.selections })
              .condition(emptyList())
              .build()

          val fieldKey = mergedField.nameWithArguments(variables)

          val base = if (key == rootKey) {
            // If we're at the root level, skip `QUERY_ROOT` altogether to save a few bytes
            null
          } else {
            key
          }

          fieldKey to replaceObjects(
              entry.value,
              mergedField,
              mergedField.type,
              base.append(fieldKey),
          )
        }.toMap()
    )

    val existingRecord = records[key]

    val mergedRecord = if (existingRecord != null) {
      /**
       * A query might contain the same object twice, we don't want to lose some fields when that happens
       */
      existingRecord.mergeWith(record).first
    } else {
      record
    }
    records[key] = mergedRecord

    return CacheKey(key)
  }


  /**
   * @param field the field currently being normalized
   * @param type_ the type currently being normalized. It can be different from [field.type] for lists.
   * Since the same field will be used for several objects in list, we can't map 1:1 anymore
   */
  private fun replaceObjects(
      value: Any?,
      field: CompiledField,
      type_: CompiledType,
      path: String,
  ): Any? {
    /**
     * Remove the NotNull decoration if needed
     */
    val type = if (type_ is CompiledNotNullType) {
      check(value != null)
      type_.ofType
    } else {
      if (value == null) {
        return null
      }
      type_
    }

    return when {
      type is CompiledListType -> {
        check(value is List<*>)
        value.mapIndexed { index, item ->
          replaceObjects(item, field, type.ofType, path.append(index.toString()))
        }
      }
      // Check for [isComposite] as we don't want to build a record for json scalars
      type is CompiledNamedType && type.isComposite() -> {
        check(value is Map<*, *>)
        @Suppress("UNCHECKED_CAST")
        val key = objectIdGenerator.cacheKeyForObject(
            value as Map<String, Any?>,
            ObjectIdGeneratorContext(field, variables),
        )?.key ?: path
        buildRecord(value, key, field.selections)
      }
      else -> {
        // scalar
        value
      }
    }
  }

  private class CollectState {
    val fields = mutableListOf<CompiledField>()
  }

  private fun collectFields(selections: List<CompiledSelection>, typename: String?, state: CollectState) {
    selections.forEach {
      when (it) {
        is CompiledField -> {
          state.fields.add(it)
        }
        is CompiledFragment -> {
          if (typename in it.possibleTypes) {
            collectFields(it.selections, typename, state)
          }
        }
      }
    }
  }

  /**
   * @param typename the typename of the object. It might be null if the `__typename` field wasn't queried. If
   * that's the case, we will collect less fields than we should and records will miss some values leading to more
   * cache miss
   */
  private fun collectFields(selections: List<CompiledSelection>, typename: String?): List<CompiledField> {
    val state = CollectState()
    collectFields(selections, typename, state)
    return state.fields
  }

  // The receiver can be null for the root query to save some space in the cache by not storing QUERY_ROOT all over the place
  private fun String?.append(next: String): String = if (this == null) next else "$this.$next"
}

