package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo3.compiler.codegen.Identifier.selections
import com.apollographql.apollo3.compiler.codegen.Identifier.serializeVariables
import com.apollographql.apollo3.compiler.codegen.Identifier.toJson
import com.apollographql.apollo3.compiler.codegen.Identifier.writer
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.java.T
import com.apollographql.apollo3.compiler.codegen.java.adapter.singletonAdapterInitializer
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Modifier

fun serializeVariablesMethodSpec(
    adapterClassName: TypeName?,
    emptyMessage: String
): MethodSpec {

  val body = if (adapterClassName == null) {
    CodeBlock.of("// $emptyMessage\n")
  } else {
    CodeBlock.of("$T.INSTANCE.$toJson($writer, $customScalarAdapters, this);\n", adapterClassName)
  }
  return MethodSpec.methodBuilder(serializeVariables)
      .addModifiers(Modifier.PUBLIC)
      .addAnnotation(JavaClassNames.Override)
      .addParameter(JavaClassNames.JsonWriter, writer)
      .addParameter(JavaClassNames.CustomScalarAdapters, customScalarAdapters)
      .addCode(body)
      .build()
}

fun adapterMethodSpec(
    adapterTypeName: TypeName,
    adaptedTypeName: TypeName
): MethodSpec {
  return MethodSpec.methodBuilder(Identifier.adapter)
      .addModifiers(Modifier.PUBLIC)
      .addAnnotation(JavaClassNames.Override)
      .returns(ParameterizedTypeName.get(JavaClassNames.Adapter, adaptedTypeName))
      .addCode(
          "return $L;\n",
          singletonAdapterInitializer(
              adapterTypeName,
              adaptedTypeName,
              false,
          )
      )
      .build()
}

fun selectionsMethodSpec(context: JavaContext, className: ClassName): MethodSpec {
  return MethodSpec.methodBuilder(selections)
      .addModifiers(Modifier.PUBLIC)
      .addAnnotation(JavaClassNames.Override)
      .returns(ParameterizedTypeName.get(JavaClassNames.List, JavaClassNames.CompiledSelection))
      .addCode("return $T.$L;\n", className, context.layout.rootSelectionsPropertyName())
      .build()
}
