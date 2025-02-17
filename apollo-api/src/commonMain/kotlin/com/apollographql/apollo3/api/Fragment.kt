package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.JsonWriter

/**
 * Base interface for a fragment implementation.
 * Fragments do not have variables per the GraphQL spec but they are inferred from arguments and used when reading the cache
 * See https://github.com/graphql/graphql-spec/issues/204 for a proposal to add fragment arguments
 */
interface Fragment<D : Fragment.Data> : Executable<D> {
  override fun serializeVariables(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters)

  override fun adapter(): Adapter<D>

  override fun selections(): List<CompiledSelection>

  /**
   * Marker interface for generated models of this fragment
   */
  interface Data : Executable.Data
}


