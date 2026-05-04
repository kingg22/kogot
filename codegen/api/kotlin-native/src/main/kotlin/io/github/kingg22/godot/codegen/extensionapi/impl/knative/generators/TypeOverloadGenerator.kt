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

/**
 * Generates Kotlin-friendly overload variants for methods whose parameters or return type
 * use a Godot type that has a Kotlin equivalent (e.g. `GodotString`↔`String`,
 * `StringName`↔`String`, `Variant`↔`Any?`).
 *
 * Configure via [TypeMapping]; the generator itself is type-agnostic.
 */
class TypeOverloadGenerator(private val typeResolver: TypeResolver) {
    companion object {
        // GodotString ↔ kotlin.String
        @JvmField
        val GodotStringMapping = TypeMapping(
            matches = { type ->
                val ctx = contextOf<Context>()
                type == ctx.classNameForOrDefault("String")
            },
            sourceType = { contextOf<Context>().classNameForOrDefault("String") },
            targetType = { STRING },
            rawSuffix = "AsGdStr",
            wrap = { name, isVararg ->
                require(!isVararg) { "Doesn't support vararg for GodotString yet" }
                CodeBlock.of("%T(%N)", contextOf<Context>().classNameForOrDefault("String"), name)
            },
            unwrapSuffix = { CodeBlock.of(".toKString()") },
        )

        /* StringName ↔ kotlin.String
        val stringNameMapping = TypeOverloadGenerator.TypeMapping(
            matches = { it == ctx.classNameForOrDefault("StringName") },
            sourceType = ctx.classNameForOrDefault("StringName"),
            targetType = STRING,
            rawSuffix = "AsSN",
            wrap = { name, isVararg ->
                require(!isVararg) { "Doesn't support vararg for StringName yet" }
                CodeBlock.of("%T(%N)", ctx.classNameForOrDefault("StringName"), name)
            },
            unwrapSuffix = ".let { GodotString(it).toKString() }",
        )

        // Variant ↔ kotlin.Any?
        val variantMapping = TypeOverloadGenerator.TypeMapping(
            matches = { it == ctx.classNameForOrDefault("Variant") },
            sourceType = ctx.classNameForOrDefault("Variant"),
            targetType = ANY.copy(nullable = true),
            rawSuffix = "AsVariant",
            wrap = { name, isVararg -> CodeBlock.of("%T.from(%N)", ctx.classNameForOrDefault("Variant"), name) },
            unwrapSuffix = ".toAny()",
        )
         */
    }

    /**
     * Describes a bidirectional substitution between a Godot type and a Kotlin type.
     *
     * @property matches      Predicate that identifies the Godot (source) type from a resolved [TypeName].
     * @property sourceType   The Godot [TypeName] used in wrap-expressions within generated code.
     * @property targetType   The Kotlin [TypeName] that replaces the source in overload signatures.
     * @property rawSuffix    Appended to the method name in the "raw" (Godot-typed) variant, e.g. `"AsGdStr"`.
     * @property wrap         Produces a [CodeBlock] that converts a target-typed argument back to source,
     *                        e.g., `GodotString(paramName)`.
     *                        Can't contain statements, only expressions.
     * @property unwrapSuffix Expression appended to a call site to convert a source return value to target,
     *                        e.g., `".toKString()"`. Must include the leading `.` if needed.
     */
    data class TypeMapping(
        val matches: context(Context) (type: TypeName) -> Boolean,
        val sourceType: context(Context) () -> TypeName,
        val targetType: context(Context) () -> TypeName,
        val rawSuffix: String,
        val wrap: context(Context) (paramName: String, isVararg: Boolean) -> CodeBlock,
        val unwrapSuffix: context(Context) () -> CodeBlock,
    )

    // ── Public API ────────────────────────────────────────────────────────────────

    /**
     * Returns which parameter indices and whether the return type of [method] match [mapping].
     */
    context(_: Context)
    fun detectMapping(method: MethodDescriptor, mapping: TypeMapping): Pair<List<Int>, Boolean> {
        val mappedIndices = method.arguments.mapIndexedNotNull { i, arg ->
            i.takeIf { mapping.matches(typeResolver.resolve(arg)) }
        }
        val hasReturnMapping = resolveReturnType(method)?.let { mapping.matches(it) } ?: false
        return mappedIndices to hasReturnMapping
    }

    /**
     * Builds all overload [FunSpec]s for [method]/[original] using [mapping].
     * Returns an empty list when no overloads are applicable.
     */
    context(_: Context)
    fun buildOverloadsForMethod(method: MethodDescriptor, original: FunSpec, mapping: TypeMapping): List<FunSpec> {
        val (mappedIndices, hasReturnMapping) = detectMapping(method, mapping)
        return buildOverloads(original, mappedIndices, hasReturnMapping, mapping)
    }

    context(_: Context)
    private fun resolveReturnType(method: MethodDescriptor): TypeName? = when (method) {
        is BuiltinClass.BuiltinMethod -> method.returnType?.let { typeResolver.resolve(it) }
        is EngineClass.ClassMethod -> method.returnValue?.let { typeResolver.resolve(it) }
        is UtilityFunction -> method.returnType?.let { typeResolver.resolve(it) }
    }

    // ── Core dispatch ─────────────────────────────────────────────────────────────

    context(_: Context)
    private fun buildOverloads(
        original: FunSpec,
        mappedIndices: List<Int>,
        hasReturnMapping: Boolean,
        mapping: TypeMapping,
    ): List<FunSpec> {
        if (mappedIndices.isEmpty() && !hasReturnMapping) return emptyList()

        val hadOperator = KModifier.OPERATOR in original.modifiers
        val base = original.withoutOperator()

        val variants = when {
            mappedIndices.isNotEmpty() && !hasReturnMapping -> paramOnlyVariants(base, mappedIndices, mapping)
            mappedIndices.isEmpty() && hasReturnMapping -> returnOnlyVariants(base, mapping)
            else -> bothVariants(base, mappedIndices, mapping)
        }

        return variants.map { it.restoreOperatorIfNeeded(hadOperator) }
    }

    // ── Variant builders ──────────────────────────────────────────────────────────

    /**
     * Only parameters are mapped; return type is unchanged.
     *
     * Emits:
     * 1. The original (source-typed params, source return)
     * 2. `inline fun [name](targetParams): SourceReturn` → delegates to original wrapping each param
     */
    context(_: Context)
    private fun paramOnlyVariants(original: FunSpec, mappedIndices: List<Int>, mapping: TypeMapping): List<FunSpec> =
        listOf(
            original,
            funWithMappedParams(original.name, original, mappedIndices, mapping)
                .addModifiers(KModifier.INLINE)
                .returns(original.returnType)
                .addCode(delegateCall(original.name, original.parameters, mappedIndices, mapping, unwrap = false))
                .build(),
        )

    /**
     * Only the return type is mapped; parameters are unchanged.
     *
     * Emits:
     * 1. `inline fun [name](sourceParams): TargetReturn` → delegates to `[name][rawSuffix]().unwrap`
     * 2. The original renamed to `[name][rawSuffix]` (source return)
     */
    context(_: Context)
    private fun returnOnlyVariants(original: FunSpec, mapping: TypeMapping): List<FunSpec> {
        val suffixName = original.name + mapping.rawSuffix
        return listOf(
            FunSpec.builder(original.name)
                .addModifiers(KModifier.INLINE)
                .returns(mapping.targetType())
                .addParameters(original.parameters)
                .addCode(delegateCall(suffixName, original.parameters, emptyList(), mapping, unwrap = true))
                .build(),
            original.toBuilder(name = suffixName).build(),
        )
    }

    /**
     * Both parameters and the return type are mapped.
     *
     * Emits:
     * 1. `inline fun [name](targetParams): TargetReturn`     → wraps params, delegates to `[name][rawSuffix]`, unwraps
     * 2. `inline fun [name][rawSuffix](targetParams): SourceReturn` → wraps params, delegates to `[name][rawSuffix]`
     * 3. The original renamed to `[name][rawSuffix]` (source-typed params and return)
     */
    context(_: Context)
    private fun bothVariants(original: FunSpec, mappedIndices: List<Int>, mapping: TypeMapping): List<FunSpec> {
        val suffixName = original.name + mapping.rawSuffix
        return listOf(
            funWithMappedParams(original.name, original, mappedIndices, mapping)
                .addModifiers(KModifier.INLINE)
                .returns(mapping.targetType())
                .addCode(delegateCall(suffixName, original.parameters, mappedIndices, mapping, unwrap = true))
                .build(),
            funWithMappedParams(suffixName, original, mappedIndices, mapping)
                .addModifiers(KModifier.INLINE)
                .returns(mapping.sourceType())
                .addCode(delegateCall(suffixName, original.parameters, mappedIndices, mapping, unwrap = false))
                .build(),
            original.toBuilder(name = suffixName).build(),
        )
    }

    // ── Shared building blocks ────────────────────────────────────────────────────

    /** Starts a [FunSpec.Builder] with [name] and parameters derived from [original], substituting mapped types. */
    context(_: Context)
    private fun funWithMappedParams(
        name: String,
        original: FunSpec,
        mappedIndices: List<Int>,
        mapping: TypeMapping,
    ): FunSpec.Builder = FunSpec.builder(name).apply {
        original.parameters.forEachIndexed { i, param ->
            val parameterSpec = if (i in mappedIndices) {
                param.toBuilder(type = mapping.targetType()).apply {
                    defaultValue(null)
                }.build()
            } else {
                param
            }
            addParameter(parameterSpec)
        }
    }

    private fun ParameterSpec.isVararg() = modifiers.contains(KModifier.VARARG)

    /**
     * Builds a `return callee(args)` or `return callee(args).unwrapSuffix` [CodeBlock].
     *
     * Parameters at [mappedIndices] are wrapped via [TypeMapping.wrap]; others are passed as-is.
     */
    context(_: Context)
    private fun delegateCall(
        callee: String,
        params: List<ParameterSpec>,
        mappedIndices: List<Int>,
        mapping: TypeMapping,
        unwrap: Boolean,
    ): CodeBlock {
        val args = params.mapIndexed { i, param ->
            val isVararg = param.isVararg()
            if (i in mappedIndices) {
                mapping.wrap(param.name, isVararg)
            } else {
                CodeBlock.of("%L%N", if (isVararg) "*" else "", param.name)
            }
        }.joinToCode(", ")
        return buildCodeBlock {
            if (unwrap) {
                addStatement("return %N(%L)%L", callee, args, mapping.unwrapSuffix())
            } else {
                addStatement("return %N(%L)", callee, args)
            }
        }
    }

    // ── FunSpec extensions ────────────────────────────────────────────────────────

    private fun FunSpec.withoutOperator(): FunSpec = if (KModifier.OPERATOR in modifiers) {
        toBuilder().apply { modifiers.remove(KModifier.OPERATOR) }.build()
    } else {
        this
    }

    private fun FunSpec.restoreOperatorIfNeeded(hadOperator: Boolean): FunSpec =
        if (hadOperator && (name == "get" || name == "set")) {
            toBuilder().addModifiers(KModifier.OPERATOR).build()
        } else {
            this
        }
}
