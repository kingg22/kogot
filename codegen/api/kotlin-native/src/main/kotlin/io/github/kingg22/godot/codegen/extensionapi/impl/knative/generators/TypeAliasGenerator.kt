package io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeAliasSpec
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.commonConfiguration
import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinClass

/**
 * Genera typealiases para versiones untyped de clases genéricas.
 *
 * Ejemplo:
 * ```kotlin
 * // builtin/GodotArray.kt
 * class GodotArray<T> { ... }
 *
 * // builtin/VariantArray.kt
 * typealias VariantArray = GodotArray<Variant>
 *
 * // builtin/GodotDictionary.kt
 * class GodotDictionary<K, V> { ... }
 *
 * // builtin/VariantDictionary.kt
 * typealias VariantDictionary = GodotDictionary<Variant, Variant>
 * ```
 *
 * ## Uso:
 * - Array sin tipo → `VariantArray` (equivalente a `Array` en GDScript)
 * - Array tipado → `GodotArray<Node>` (equivalente a `Array[Node]` en GDScript)
 * - Dictionary sin tipo → `VariantDictionary` (equivalente a `Dictionary` en GDScript)
 * - Dictionary tipado → `GodotDictionary<String, Int>` (equivalente a typed dict en GDScript)
 */
class TypeAliasGenerator(private val genericInterceptor: GenericBuiltinInterceptor) {

    context(context: Context)
    fun generateFile(builtinClass: BuiltinClass): FileSpec? {
        val spec = generateTypeAliasSpec(builtinClass) ?: return null
        return FileSpec
            .builder(context.packageForOrDefault(builtinClass.name), spec.name)
            .commonConfiguration()
            .addTypeAlias(spec)
            .build()
    }

    context(context: Context)
    fun generateTypeAliasSpec(builtinClass: BuiltinClass): TypeAliasSpec? {
        if (!genericInterceptor.requiresGenerics(builtinClass)) return null

        val genericConfig = genericInterceptor.getGenericConfig(builtinClass) ?: return null
        val (aliasName, aliasType) = genericConfig.untypedAlias ?: return null

        return TypeAliasSpec
            .builder(aliasName, aliasType)
            .addKdocForTypeAlias(aliasName, aliasType)
            .build()
    }

    private fun TypeAliasSpec.Builder.addKdocForTypeAlias(aliasName: String, aliasType: ParameterizedTypeName) = apply {
        when (aliasName) {
            "VariantArray" -> {
                addKdoc(
                    """
                        Untyped array, equivalent to `Array` in GDScript.

                        For typed arrays, use `%T<T>` instead.

                        ## Examples:
                        ```kotlin
                        val untyped: VariantArray = VariantArray()  // Any Variant type
                        val typed: GodotArray<Node> = GodotArray()  // Only Node elements
                        ```
                    """.trimIndent(),
                    aliasType.rawType,
                )
            }

            "VariantDictionary" -> {
                addKdoc(
                    """
                        Untyped dictionary, equivalent to `Dictionary` in GDScript.

                        For typed dictionaries, use `%T<K, V>` instead.

                        ## Examples:
                        ```kotlin
                        val untyped: VariantDictionary = VariantDictionary()  // Any Variant key/value
                        val typed: GodotDictionary<String, Int> = GodotDictionary()  // String keys, Int values
                        ```
                    """.trimIndent(),
                    aliasType.rawType,
                )
            }
        }
    }
}
