package io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators.interceptors

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import io.github.kingg22.godot.codegen.extensionapi.Context

/** A parameter named [name] of type `ElementCodec<[element]>`. */
context(ctx: Context)
fun codecParameter(name: String, element: String): ParameterSpec {
    val codec = ClassName(ctx.packageForOrDefault("Variant"), "ElementCodec")
    return ParameterSpec.builder(name, codec.parameterizedBy(TypeVariableName(element))).build()
}

/** The handwritten collection view [name] over [elements], in the builtin package. */
context(ctx: Context)
fun collectionBase(name: String, vararg elements: String): TypeName =
    ClassName(ctx.packageForOrDefault("Variant"), name)
        .parameterizedBy(elements.map { TypeVariableName(it) })
