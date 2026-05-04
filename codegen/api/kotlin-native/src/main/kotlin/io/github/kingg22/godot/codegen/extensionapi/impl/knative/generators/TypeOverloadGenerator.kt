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
import io.github.kingg22.godot.codegen.models.extensionapi.MethodArg
import io.github.kingg22.godot.codegen.models.extensionapi.MethodDescriptor
import io.github.kingg22.godot.codegen.models.extensionapi.UtilityFunction

/**
 * Generates Kotlin-friendly overload variants for methods/constructors whose parameters or return type
 * use a Godot type that has a Kotlin equivalent (e.g. `GodotString`↔`String`,
 * `StringName`↔`String`, `Variant`↔`Any?`).
 *
 * Configure via [TypeMapping]; the generator itself is type-agnostic.
 */
class TypeOverloadGenerator(private val typeResolver: TypeResolver) {
    companion object {
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
            transformDefaultValue = { arg, _ ->
                // Godot string defaults arrive quoted: "{_}" → we want the Kotlin literal "{_}".
                // Strip the surrounding Godot quotes and re-emit as a Kotlin string literal via %S.
                arg.defaultValue
                    ?.takeIf { it.startsWith('"') && it.endsWith('"') }
                    ?.let { CodeBlock.of("%S", it.removeSurrounding("\"")) }
            },
            configureConstructor = { _, args ->
                callThisConstructor(args)
            },
        )
    }

    /**
     * Describes a bidirectional substitution between a Godot type and a Kotlin type.
     *
     * @property matches               Predicate that identifies the Godot (source) type from a resolved [TypeName].
     * @property sourceType            The Godot [TypeName] used in wrap-expressions within generated code.
     * @property targetType            The Kotlin [TypeName] that replaces the source in overload signatures.
     * @property rawSuffix             Appended to the method name in the "raw" (Godot-typed) variant, e.g. `"AsGdStr"`.
     * @property wrap                  Produces a [CodeBlock] converting a target-typed argument back to source.
     *                                 Cannot contain statements, only expressions.
     * @property unwrapSuffix          Expression appended to a call site to convert a source return value to target,
     *                                 e.g. `".toKString()"`. Must include the leading `.` if needed.
     * @property transformDefaultValue Adapts the default value of a mapped parameter for the target type.
     *                                 Receives the original [MethodArg] (with raw Godot default string) and the
     *                                 resolved target [TypeName]. Return `null` to drop the default — never return
     *                                 the source-typed CodeBlock from the original [ParameterSpec], as that would
     *                                 reproduce the bug this field exists to fix.
     */
    data class TypeMapping(
        val matches: context(Context) (type: TypeName) -> Boolean,
        val sourceType: context(Context) () -> TypeName,
        val targetType: context(Context) () -> TypeName,
        val rawSuffix: String,
        val wrap: context(Context) (paramName: String, isVararg: Boolean) -> CodeBlock,
        val unwrapSuffix: context(Context) () -> CodeBlock,
        val transformDefaultValue: context(Context) (arg: MethodArg, targetType: TypeName) -> CodeBlock? =
            { _, _ -> null },
        val configureConstructor: context(Context) FunSpec.Builder.(
            mappedIndices: List<Int>,
            wrappedArgs: List<CodeBlock>,
        ) -> Unit = { _, _ -> },
    )

    // ── Public API ────────────────────────────────────────────────────────────────

    /**
     * Builds all overload [FunSpec]s for [method]/[original] using [mapping].
     * Returns an empty list when no overloads are applicable.
     */
    context(_: Context)
    fun buildOverloadsForMethod(method: MethodDescriptor, original: FunSpec, mapping: TypeMapping): List<FunSpec> {
        val mappedIndices = detectMapping(method.arguments, mapping)
        val hasReturnMapping = detectReturnTypeMapping(method, mapping)
        return buildOverloads(original, mappedIndices, hasReturnMapping, mapping, method.arguments)
    }

    /**
     * Builds a constructor overload for [constructor]/[original] using [mapping], or an empty list if no
     * parameters match.
     *
     * The [configure] lambda receives a [FunSpec.Builder] already populated with target-typed parameters
     * (with adapted defaults) and the pre-built `wrappedArgs` per mapped param. Use them to set up
     * delegation and/or the constructor body.
     *
     * ```kotlin
     * generator.buildOverloadsForConstructor(method, original, GodotStringMapping) { _, wrappedArgs ->
     *     callThisConstructor(CodeBlock.of("null"))
     *     addCode(buildCodeBlock {
     *         beginControlFlow("memScoped")
     *         addStatement("fptr.invoke(rawPtr, allocConstTypePtrArray(%L))", wrappedArgs.joinToCode(", "))
     *         endControlFlow()
     *     })
     * }
     * ```
     */
    context(_: Context)
    fun buildOverloadsForConstructor(
        constructor: BuiltinClass.Constructor,
        original: FunSpec,
        mapping: TypeMapping,
    ): FunSpec? {
        val mappedIndices = detectMapping(constructor.arguments, mapping)
        if (mappedIndices.isEmpty()) return null

        val wrappedArgs = buildWrappedArgs(original.parameters, mappedIndices, mapping)
        val builder = constructorWithMappedParams(original, mappedIndices, mapping, constructor.arguments)

        with(mapping) {
            builder.configureConstructor(mappedIndices, wrappedArgs)
        }

        return builder.build()
    }

    // ── Core dispatch ─────────────────────────────────────────────────────────────

    /**
     * Returns which parameter indices match [mapping].
     */
    context(_: Context)
    private fun detectMapping(arguments: List<MethodArg>, mapping: TypeMapping): List<Int> {
        val mappedIndices = arguments.mapIndexedNotNull { i, arg ->
            i.takeIf { mapping.matches(typeResolver.resolve(arg)) }
        }
        return mappedIndices
    }

    context(_: Context)
    private fun detectReturnTypeMapping(method: MethodDescriptor, mapping: TypeMapping): Boolean =
        resolveReturnType(method)?.let { mapping.matches(it) } ?: false

    context(_: Context)
    private fun resolveReturnType(method: MethodDescriptor): TypeName? = when (method) {
        is BuiltinClass.BuiltinMethod -> method.returnType?.let { typeResolver.resolve(it) }
        is EngineClass.ClassMethod -> method.returnValue?.let { typeResolver.resolve(it) }
        is UtilityFunction -> method.returnType?.let { typeResolver.resolve(it) }
    }

    context(_: Context)
    private fun buildOverloads(
        original: FunSpec,
        mappedIndices: List<Int>,
        hasReturnMapping: Boolean,
        mapping: TypeMapping,
        methodArgs: List<MethodArg>,
    ): List<FunSpec> {
        if (mappedIndices.isEmpty() && !hasReturnMapping) return emptyList()

        val hadOperator = KModifier.OPERATOR in original.modifiers
        val base = original.withoutOperator()

        val variants = when {
            mappedIndices.isNotEmpty() && !hasReturnMapping -> paramOnlyVariants(
                base,
                mappedIndices,
                mapping,
                methodArgs,
            )

            mappedIndices.isEmpty() && hasReturnMapping -> returnOnlyVariants(base, mapping)

            else -> bothVariants(base, mappedIndices, mapping, methodArgs)
        }

        return variants.map { it.restoreOperatorIfNeeded(hadOperator) }
    }

    // ── Variant builders ──────────────────────────────────────────────────────────

    /**
     * Only parameters are mapped; return type is unchanged.
     *
     * Emits:
     * 1. The original (source-typed params, source return)
     * 2. `inline fun [name](targetParams): SourceReturn` — delegates to original, wrapping each mapped param
     */
    context(_: Context)
    private fun paramOnlyVariants(
        original: FunSpec,
        mappedIndices: List<Int>,
        mapping: TypeMapping,
        methodArgs: List<MethodArg>,
    ): List<FunSpec> = listOf(
        original,
        funWithMappedParams(original.name, original, mappedIndices, mapping, methodArgs)
            .makeInlineIfPossible()
            .makeFinal()
            .returns(original.returnType)
            .addCode(delegateCall(original.name, original.parameters, mappedIndices, mapping, unwrap = false))
            .build(),
    )

    /**
     * Only the return type is mapped; parameters are unchanged — no default adaptation needed.
     *
     * Emits:
     * 1. `inline fun [name](sourceParams): TargetReturn` — delegates to `[name][rawSuffix]().unwrap`
     * 2. The original renamed to `[name][rawSuffix]` (source return)
     */
    context(_: Context)
    private fun returnOnlyVariants(original: FunSpec, mapping: TypeMapping): List<FunSpec> {
        val suffixName = original.name + mapping.rawSuffix
        return listOf(
            FunSpec.builder(original.name)
                .makeInlineIfPossible()
                .makeFinal()
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
     * 1. `inline fun [name](targetParams): TargetReturn`          — wraps params, unwraps result
     * 2. `inline fun [name][rawSuffix](targetParams): SourceReturn` — wraps params only
     * 3. The original renamed to `[name][rawSuffix]` (source-typed params and return, defaults preserved)
     */
    context(_: Context)
    private fun bothVariants(
        original: FunSpec,
        mappedIndices: List<Int>,
        mapping: TypeMapping,
        methodArgs: List<MethodArg>,
    ): List<FunSpec> {
        val suffixName = original.name + mapping.rawSuffix
        return listOf(
            funWithMappedParams(original.name, original, mappedIndices, mapping, methodArgs)
                .makeInlineIfPossible()
                .makeFinal()
                .returns(mapping.targetType())
                .addCode(delegateCall(suffixName, original.parameters, mappedIndices, mapping, unwrap = true))
                .build(),
            funWithMappedParams(suffixName, original, mappedIndices, mapping, methodArgs)
                .makeInlineIfPossible()
                .makeFinal()
                .returns(mapping.sourceType())
                .addCode(delegateCall(suffixName, original.parameters, mappedIndices, mapping, unwrap = false))
                .build(),
            original.toBuilder(name = suffixName).build(),
        )
    }

    // ── Shared building blocks ────────────────────────────────────────────────────

    /**
     * Starts a [FunSpec.Builder] with [name] and parameters from [original], substituting mapped types
     * and adapting default values via [TypeMapping.transformDefaultValue].
     *
     * Unmapped parameters are copied as-is; mapped parameters are rebuilt from scratch (rather than via
     * [ParameterSpec.toBuilder]) so we can control whether a default value is emitted — KotlinPoet
     * always copies the existing default in [ParameterSpec.toBuilder] with no way to clear it.
     */
    context(_: Context)
    private fun funWithMappedParams(
        name: String,
        original: FunSpec,
        mappedIndices: List<Int>,
        mapping: TypeMapping,
        methodArgs: List<MethodArg>,
    ): FunSpec.Builder = original.toBuilder(name).apply {
        clearBody() // we'll go to use all the original FunSpec, but we don't want to copy the body
        parameters.clear()
        original.parameters.forEachIndexed { i, param ->
            addParameter(
                if (i in mappedIndices) {
                    adaptParameter(param, mapping, methodArgs.getOrNull(i))
                } else {
                    param
                },
            )
        }
    }

    /**
     * Like [funWithMappedParams] but produces a constructor builder, copying modifiers from [original].
     * `INLINE` is stripped — constructors do not support that modifier.
     */
    context(_: Context)
    private fun constructorWithMappedParams(
        original: FunSpec,
        mappedIndices: List<Int>,
        mapping: TypeMapping,
        methodArgs: List<MethodArg>,
    ): FunSpec.Builder = original.toBuilder().apply {
        clearBody()
        parameters.clear()
        makeFinal()
        original.parameters.forEachIndexed { i, param ->
            addParameter(
                if (i in mappedIndices) {
                    adaptParameter(param, mapping, methodArgs.getOrNull(i))
                } else {
                    param
                },
            )
        }
    }

    /**
     * Rebuilds [original] with [mapping]'s target type and an adapted default value.
     *
     * We construct a fresh [ParameterSpec] instead of using [ParameterSpec.toBuilder] because
     * [ParameterSpec.toBuilder] always copies [ParameterSpec.defaultValue] and KotlinPoet exposes no
     * public API to clear it afterwards. Building fresh lets us apply [TypeMapping.transformDefaultValue]
     * cleanly: if it returns `null` the overload parameter simply has no default, which is correct
     * (the source-typed original still carries the default for callers that need it).
     */
    context(_: Context)
    private fun adaptParameter(original: ParameterSpec, mapping: TypeMapping, methodArg: MethodArg?): ParameterSpec {
        val targetType = mapping.targetType()
        val adaptedDefault = methodArg?.let { mapping.transformDefaultValue(it, targetType) }
        return original.toBuilder(type = targetType)
            .apply { adaptedDefault?.let { defaultValue(it) } }
            .build()
    }

    /**
     * Pre-builds the argument [CodeBlock]s for a delegate call, wrapping mapped params via [TypeMapping.wrap]
     * and spreading varargs. Shared between [delegateCall] and [buildOverloadsForConstructor].
     */
    context(_: Context)
    private fun buildWrappedArgs(
        params: List<ParameterSpec>,
        mappedIndices: List<Int>,
        mapping: TypeMapping,
    ): List<CodeBlock> = params.mapIndexed { i, param ->
        if (i in mappedIndices) {
            mapping.wrap(param.name, param.isVararg())
        } else {
            CodeBlock.of("%L%N", if (param.isVararg()) "*" else "", param.name)
        }
    }

    context(_: Context)
    private fun delegateCall(
        callee: String,
        params: List<ParameterSpec>,
        mappedIndices: List<Int>,
        mapping: TypeMapping,
        unwrap: Boolean,
    ): CodeBlock {
        val args = buildWrappedArgs(params, mappedIndices, mapping).joinToCode(", ")
        return buildCodeBlock {
            if (unwrap) {
                addStatement("return %N(%L)%L", callee, args, mapping.unwrapSuffix())
            } else {
                addStatement("return %N(%L)", callee, args)
            }
        }
    }

    // ── FunSpec / ParameterSpec extensions ────────────────────────────────────────

    private fun FunSpec.withoutOperator(): FunSpec = if (KModifier.OPERATOR in modifiers) {
        toBuilder().apply { modifiers.remove(KModifier.OPERATOR) }.build()
    } else {
        this
    }

    @IgnorableReturnValue
    private fun FunSpec.restoreOperatorIfNeeded(hadOperator: Boolean): FunSpec =
        if (hadOperator && (name == "get" || name == "set")) {
            toBuilder().addModifiers(KModifier.OPERATOR).build()
        } else {
            this
        }

    @IgnorableReturnValue
    private fun FunSpec.Builder.makeInlineIfPossible() = apply {
        if (KModifier.INLINE !in modifiers && KModifier.OPEN !in modifiers) addModifiers(KModifier.INLINE)
    }

    @IgnorableReturnValue
    private fun FunSpec.Builder.makeFinal() = apply {
        modifiers.remove(KModifier.OPEN)
    }

    private fun ParameterSpec.isVararg() = KModifier.VARARG in modifiers
}
