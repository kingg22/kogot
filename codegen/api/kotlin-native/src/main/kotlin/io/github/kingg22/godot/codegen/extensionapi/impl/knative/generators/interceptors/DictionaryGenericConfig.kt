package io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators.interceptors

import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators.GenericBuiltinInterceptor

class DictionaryGenericConfig : GenericBuiltinInterceptor.GenericConfig {
    override val typeVariableNames: List<String> = listOf("K", "V")

    context(context: Context)
    override val superclass: TypeName get() = collectionBase("GodotMap", "K", "V")

    context(context: Context)
    override val codecParameters: List<ParameterSpec>
        get() = listOf(codecParameter("keyCodec", "K"), codecParameter("valueCodec", "V"))

    override val kotlinCollectionEquality: Boolean = true

    override val viewMethods: Map<String, String> = mapOf(
        "size" to "size",
        "is_empty" to "isEmpty",
        "clear" to "clear",
        "has" to "has",
        "erase" to "erase",
        "get" to "getVariant",
        "set" to "setVariant",
        "keys" to "keys",
    )

    context(context: Context)
    override val untypedAlias: Pair<String, ParameterizedTypeName>
        get() {
            val godotDictClass = context.classNameForOrDefault("Dictionary", typedClass = true)
            val variantClass = context.classNameForOrDefault("Variant")
            val untypedDict = godotDictClass.parameterizedBy(variantClass, variantClass)
            return "VariantDictionary" to untypedDict
        }

    // NO transformar tipos de parámetros - Dictionary<K, V> internamente usa Variant
    // Los factory methods (ofIntInt, etc.) se generan aparte
}
