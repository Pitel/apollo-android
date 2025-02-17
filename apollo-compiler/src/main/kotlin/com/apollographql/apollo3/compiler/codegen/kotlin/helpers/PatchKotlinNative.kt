package com.apollographql.apollo3.compiler.codegen.kotlin.helpers

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

internal fun TypeSpec.patchKotlinNativeOptionalArrayProperties(): TypeSpec {
  if (kind != TypeSpec.Kind.CLASS) {
    return this
  }

  val patchedNestedTypes = typeSpecs.map { type ->
    if (type.kind == TypeSpec.Kind.CLASS) {
      type.patchKotlinNativeOptionalArrayProperties()
    } else {
      type
    }
  }

  val nonOptionalListPropertyAccessors = propertySpecs
      .filter { propertySpec ->
        val propertyType = propertySpec.type
        propertyType is ParameterizedTypeName &&
            propertyType.rawType == List::class.asClassName() &&
            propertyType.typeArguments.single().isNullable
      }
      .map { propertySpec ->
        val listItemType = (propertySpec.type as ParameterizedTypeName).typeArguments.single().copy(nullable = false)
        val nonOptionalListType = List::class.asClassName().parameterizedBy(listItemType).copy(nullable = propertySpec.type.isNullable)
        FunSpec
            .builder("${propertySpec.name}FilterNotNull")
            .returns(nonOptionalListType)
            .addStatement("return %L%L.filterNotNull()", propertySpec.name, if (propertySpec.type.isNullable) "?" else "")
            .build()
      }
  return toBuilder()
      .addFunctions(nonOptionalListPropertyAccessors)
      .apply { typeSpecs.clear() }
      .addTypes(patchedNestedTypes)
      .build()
}