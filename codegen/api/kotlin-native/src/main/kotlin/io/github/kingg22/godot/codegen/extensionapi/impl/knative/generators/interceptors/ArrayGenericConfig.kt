package io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators.interceptors

import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators.GenericBuiltinInterceptor

class ArrayGenericConfig : GenericBuiltinInterceptor.GenericConfig {
    override val typeVariableNames: List<String> = listOf("T")

    context(context: Context)
    override val superclass: TypeName get() = collectionBase("GodotList", "T")

    context(context: Context)
    override val codecParameters: List<ParameterSpec> get() = listOf(codecParameter("codec", "T"))

    override val viewMethods: Map<String, String> = mapOf(
        "size" to "size",
        "is_empty" to "isEmpty",
        "clear" to "clear",
        "get" to "getVariant",
        "set" to "setVariant",
        "insert" to "insert",
        "remove_at" to "removeAt",
    )

    context(context: Context)
    override val untypedAlias: Pair<String, ParameterizedTypeName>
        get() {
            val godotArrayClass = context.classNameForOrDefault("Array", "GodotArray")
            val variantClass = context.classNameForOrDefault("Variant")
            val untypedArray = godotArrayClass.parameterizedBy(variantClass)
            return "VariantArray" to untypedArray
        }

    // NO transformar tipos de parámetros - Array<T> internamente usa Variant
    // Los factory methods (ofInt, ofLong, etc.) se generan aparte
}
