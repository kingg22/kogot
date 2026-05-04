package io.github.kingg22.godot.codegen.extensionapi.impl.knative.generators

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.joinToCode
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinClass
import io.github.kingg22.godot.codegen.models.extensionapi.EngineClass
import io.github.kingg22.godot.codegen.models.extensionapi.MethodDescriptor
import io.github.kingg22.godot.codegen.models.extensionapi.UtilityFunction

class StringOverloadGenerator(private val typeResolver: TypeResolver) {

    /**
     * Returns true if this [TypeName] refers to [GodotString][Context.classNameForOrDefault].
     */
    context(ctx: Context)
    private fun TypeName.isGodotString(): Boolean = this == ctx.classNameForOrDefault("String")

    /**
     * Detects which parameters and the return type of a [MethodDescriptor] are [GodotString][Context.classNameForOrDefault].
     *
     * @return A pair of (godotStringParamIndices: List<Int>, hasStringReturn: Boolean)
     */
    context(context: Context)
    fun detectGodotStringParamsAndReturn(method: MethodDescriptor): Pair<List<Int>, Boolean> {
        val godotStringIndices = method.arguments
            .mapIndexedNotNull { index, arg ->
                val resolved = typeResolver.resolve(arg)
                if (resolved.isGodotString()) index else null
            }

        val hasStringReturn = when (method) {
            is BuiltinClass.BuiltinMethod ->
                method.returnType?.let { typeResolver.resolve(it).isGodotString() } ?: false

            is EngineClass.ClassMethod ->
                method.returnValue?.let { typeResolver.resolve(it).isGodotString() } ?: false

            is UtilityFunction ->
                method.returnType?.let { typeResolver.resolve(it).isGodotString() } ?: false
        }

        return godotStringIndices to hasStringReturn
    }

    /**
     * Builds String overload [FunSpec]s for a [MethodDescriptor] if any parameter or return type is `GodotString`.
     *
     * Calls [buildStringOverloads] internally after detecting GodotString indices via [detectGodotStringParamsAndReturn].
     *
     * @param method     The method descriptor
     * @param original  The already-built [FunSpec] for the original method
     * @return List of overload [FunSpec]s, empty if no String overloads are needed
     */
    context(context: Context)
    fun buildStringOverloadsForMethod(method: MethodDescriptor, original: FunSpec): List<FunSpec> {
        val (godotStringIndices, hasStringReturn) = detectGodotStringParamsAndReturn(method)
        return buildStringOverloads(original, godotStringIndices, hasStringReturn)
    }

    /**
     * Builds the String overload variants for a method that takes or returns `GodotString`.
     *
     * Overload variants:
     * - **Variant 2** (only param transform, no suffix): `inline fun name(param: String): String`
     * - **Variant 3** (only return transform, suffix `AsGdStr`): `fun nameAsGdStr(param: GodotString): String`
     * - **Variant 4** (both transforms, suffix `AsGdStr`): `inline fun nameAsGdStr(param: String): String`
     *
     * @param original         The original [FunSpec] that takes/returns GodotString
     * @param godotStringIndices Indices of parameters that are GodotString
     * @param hasStringReturn  Whether the return type is GodotString
     * @return List of overload [FunSpec]s (empty if no String overloads needed)
     */
    context(context: Context)
    private fun buildStringOverloads(
        original: FunSpec,
        godotStringIndices: List<Int>,
        hasStringReturn: Boolean,
    ): List<FunSpec> {
        val original = original.toBuilder().apply { modifiers.remove(KModifier.OPERATOR) }.build()
        val hasStringParam = godotStringIndices.isNotEmpty()

        // Nothing to do if neither param nor return is GodotString
        if (!hasStringParam && !hasStringReturn) return emptyList()

        // Detect vararg - vararg methods are complex, skip overloads for now
        val isVararg = original.parameters.isNotEmpty() &&
            original.parameters.last().type == context.classNameFor("Variant") &&
            original.parameters.last().modifiers.contains(KModifier.VARARG)
        if (isVararg) return emptyList()

        val godotStringClass = context.classNameForOrDefault("String")
        val kotlinStringClass = STRING
        val overloads = mutableListOf<FunSpec>()
        val originalName = original.name

        // ── Variant 2: param→String, return unchanged, same name, inline ─────────────
        // When hasStringReturn=false: param→String, return→GodotString, same name, inline
        // When hasStringReturn=true: param→String, return→kotlin.String, same name, inline
        if (hasStringParam && !hasStringReturn) {
            val builder = FunSpec
                .builder(originalName)
                .addModifiers(KModifier.INLINE)
                .returns(original.returnType)

            // Parameters: GodotString params become String, others unchanged
            original.parameters.forEachIndexed { index, param ->
                val newType = if (index in godotStringIndices) kotlinStringClass else param.type
                builder.addParameter(ParameterSpec.builder(param.name, newType).build())
            }

            // Body: delegate to original with GodotString wrapping
            val argsList = original.parameters.mapIndexed { index, param ->
                if (index in godotStringIndices) {
                    CodeBlock.of("%T(%N)", godotStringClass, param.name)
                } else {
                    CodeBlock.of("%N", param.name)
                }
            }
            builder.addCode(buildCodeBlock { addStatement("return %N(%L)", originalName, argsList.joinToCode(", ")) })
            overloads.add(original)
            overloads.add(builder.build())
        }

        // ── Variant 3: hasStringReturn=true → TWO variants ─────────────────────────
        // 3a. Non-suffix: returns kotlin.String, delegates to AsGdStr and converts
        // 3b. AsGdStr suffix: returns GodotString (renamed original)
        if (hasStringReturn && !hasStringParam) {
            // 3a. Non-suffix (inline): return kotlin.String, delegates to AsGdStr().toKString()
            val nonSuffixBuilder = FunSpec
                .builder(originalName)
                .addModifiers(KModifier.INLINE)
                .returns(kotlinStringClass)

            original.parameters.forEach { nonSuffixBuilder.addParameter(it) }

            val argsList = original.parameters.map { CodeBlock.of("%N", it.name) }
            nonSuffixBuilder.addCode(
                buildCodeBlock {
                    addStatement("return %N(%L).toKString()", "${originalName}AsGdStr", argsList.joinToCode(", "))
                },
            )
            overloads.add(nonSuffixBuilder.build())

            // 3b. AsGdStr suffix: return GodotString (renamed original method)
            val asGdStrBuilder = original.toBuilder(name = "${originalName}AsGdStr")
            overloads.add(asGdStrBuilder.build())
        }

        // ── Variant 4: param→String, return transformed, suffix AsGdStr, inline ─────
        if (hasStringParam && hasStringReturn) {
            // 4a. Non-suffix (inline): param→String, return→kotlin.String
            //     Delegates to AsGdStr variant with toKString()
            val nonSuffixBuilder = FunSpec
                .builder(originalName)
                .addModifiers(KModifier.INLINE)
                .returns(kotlinStringClass)

            original.parameters.forEachIndexed { index, param ->
                val newType = if (index in godotStringIndices) kotlinStringClass else param.type
                nonSuffixBuilder.addParameter(ParameterSpec.builder(param.name, newType).build())
            }

            val nonSuffixArgs = original.parameters.mapIndexed { index, param ->
                if (index in godotStringIndices) {
                    CodeBlock.of("%T(%N)", godotStringClass, param.name)
                } else {
                    CodeBlock.of("%N", param.name)
                }
            }
            nonSuffixBuilder.addCode(
                buildCodeBlock {
                    addStatement("return %N(%L).toKString()", "${originalName}AsGdStr", nonSuffixArgs.joinToCode(", "))
                },
            )
            overloads.add(nonSuffixBuilder.build())

            // 4b. AsGdStr suffix (inline): param→String, return→GodotString
            //     Wraps params in GodotString and delegates to AsGdStr variant
            val asGdStrBuilder = FunSpec
                .builder("${originalName}AsGdStr")
                .addModifiers(KModifier.INLINE)
                .returns(godotStringClass)

            original.parameters.forEachIndexed { index, param ->
                val newType = if (index in godotStringIndices) kotlinStringClass else param.type
                asGdStrBuilder.addParameter(ParameterSpec.builder(param.name, newType).build())
            }

            val asGdStrArgs = original.parameters.mapIndexed { index, param ->
                if (index in godotStringIndices) {
                    CodeBlock.of("%T(%N)", godotStringClass, param.name)
                } else {
                    CodeBlock.of("%N", param.name)
                }
            }
            asGdStrBuilder.addCode(
                buildCodeBlock {
                    addStatement("return %N(%L)", "${originalName}AsGdStr", asGdStrArgs.joinToCode(", "))
                },
            )
            overloads.add(asGdStrBuilder.build())
            overloads.add(original.toBuilder(name = originalName + "AsGdStr").build())
        }

        return overloads
    }
}
