package io.github.kingg22.godot.codegen.impl.extensionapi.native.generators

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeAliasSpec
import io.github.kingg22.godot.codegen.impl.commonConfiguration
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinClass

/**
 * Genera typealiases para versiones untyped de clases genéricas.
 *
 * Ejemplo:
 * ```kotlin
 * // builtin/GodotArray.kt
 * class GodotArray<T> { ... }
 *
 * // builtin/VariantArray.kt (este archivo)
 * typealias VariantArray = GodotArray<Variant>
 * ```
 *
 * ## Uso:
 * - Array sin tipo → `VariantArray` (equivalente a `Array` en GDScript)
 * - Array tipado → `GodotArray<Node>` (equivalente a `Array[Node]` en GDScript)
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
        if (aliasName == "VariantArray") {
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
        // TODO add Dictionary
    }
}
