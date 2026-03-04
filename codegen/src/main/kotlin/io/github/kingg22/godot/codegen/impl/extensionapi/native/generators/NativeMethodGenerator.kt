package io.github.kingg22.godot.codegen.impl.extensionapi.native.generators

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import io.github.kingg22.godot.codegen.impl.addKdocForBitfield
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinClass
import io.github.kingg22.godot.codegen.models.extensionapi.GodotClass
import io.github.kingg22.godot.codegen.models.extensionapi.MethodArg
import io.github.kingg22.godot.codegen.models.extensionapi.TypeMetaHolder

/**
 * Shared method/parameter generation logic used by both builtin class
 * generators and engine class generators.
 *
 * Knows nothing about class structure, operators, or statics —
 * those are concerns of the specific generators.
 *
 * @param body provides CodeBlock bodies (stub TODO() or real cinterop calls)
 */
class NativeMethodGenerator(private val typeResolver: TypeResolver, private val body: BodyGenerator) {

    /**
     * Builds a [FunSpec] from raw method data.
     *
     * @param name           Godot method name (will be passed through [safeIdentifier])
     * @param returnType     Godot return type string, null → Unit
     * @param isVararg       whether the method accepts trailing vararg Variant args
     * @param arguments      fixed argument list
     * @param extraModifiers additional [KModifier]s (e.g., OVERRIDE, OPERATOR)
     */
    context(context: Context)
    fun buildMethod(
        name: String,
        returnType: TypeName,
        isVararg: Boolean,
        arguments: List<MethodArg>,
        extraModifiers: List<KModifier> = emptyList(),
        methodKdoc: String? = null,
    ): FunSpec {
        val kotlinName = safeIdentifier(name)
        val builder = FunSpec
            .builder(kotlinName)
            .addModifiers(extraModifiers)
            .returns(returnType)
            .addCode(body.todoBody())
            .apply {
                if (!methodKdoc.isNullOrEmpty()) {
                    addKdoc("%S", methodKdoc.replace("/*", "").replace("*/", ""))
                }
                if (name != kotlinName) addKdoc("\nOriginal name: `%L`", name)
            }.fixAccidentalOverride(name, returnType)

        // Fixed args always come first
        arguments.forEach { arg ->
            require(!isVararg || safeIdentifier(arg.name) != "args") {
                "Vararg method '$name' has a fixed arg named 'args' — rename it to avoid clash"
            }
            builder.addParameter(buildParameter(arg))
        }

        // Trailing vararg only after all fixed args
        if (isVararg) {
            builder.addParameter(
                ParameterSpec
                    .builder("args", context.classNameFor("Variant"), KModifier.VARARG)
                    .build(),
            )
        }

        return builder.build()
    }

    context(context: Context)
    private fun FunSpec.Builder.fixAccidentalOverride(name: String, returnType: TypeName): FunSpec.Builder {
        when (name) {
            "to_string" if returnType == context.classNameFor("String", "GodotString") -> {
                println("INFO: renaming toString() → toGodotString() to avoid Any clash")
                return build()
                    .toBuilder("toGodotString")
                    .addKdoc(
                        "Generated Note: Original name was `toString`, renamed to avoid conflict with [Any.toString].",
                    )
            }

            else -> return this
        }
    }

    /**
     * Builds a [FunSpec] from raw method data.
     *
     * @param name           Godot method name (will be passed through [safeIdentifier])
     * @param returnType     Godot return type string, null → Unit
     * @param isVararg       whether the method accepts trailing vararg Variant args
     * @param arguments      fixed argument list
     * @param extraModifiers additional [KModifier]s (e.g., OVERRIDE, OPERATOR)
     */
    context(context: Context)
    fun buildMethod(
        name: String,
        returnType: TypeMetaHolder?,
        isVararg: Boolean,
        arguments: List<MethodArg>,
        extraModifiers: List<KModifier> = emptyList(),
        methodKdoc: String? = null,
    ): FunSpec {
        val returnTypeName = returnType?.let { typeResolver.resolve(it) } ?: UNIT
        return buildMethod(name, returnTypeName, isVararg, arguments, extraModifiers, methodKdoc)
    }

    /**
     * Builds a [FunSpec] from raw method data.
     *
     * @param name           Godot method name (will be passed through [safeIdentifier])
     * @param returnType     Godot return type string, null → Unit
     * @param isVararg       whether the method accepts trailing vararg Variant args
     * @param arguments      fixed argument list
     * @param extraModifiers additional [KModifier]s (e.g., OVERRIDE, OPERATOR)
     */
    context(context: Context)
    fun buildMethod(
        name: String,
        returnType: String?,
        isVararg: Boolean,
        arguments: List<MethodArg>,
        extraModifiers: List<KModifier> = emptyList(),
        methodKdoc: String? = null,
    ): FunSpec {
        val returnTypeName = returnType?.let { typeResolver.resolve(it) } ?: UNIT
        return buildMethod(name, returnTypeName, isVararg, arguments, extraModifiers, methodKdoc)
    }

    /**
     * Builds a [ParameterSpec] for a single [MethodArg].
     *
     * Default values from Godot JSON are emitted as `TODO()` —
     * the impl layer replaces them with actual expressions.
     */
    context(_: Context)
    fun buildParameter(arg: MethodArg): ParameterSpec {
        val type = typeResolver.resolve(arg)
        val kotlinName = safeIdentifier(arg.name)
        val paramBuilder = ParameterSpec
            .builder(kotlinName, type)
            .addKdocForBitfield(arg.type)
        if (arg.name != kotlinName) paramBuilder.addKdoc("Original name: `%L`", arg.name)
        arg.defaultValue?.let { value ->
            // FIXME remove kdoc when is implemented
            paramBuilder.addKdoc("Default value: `%L`", value)
            paramBuilder.defaultValue(body.todoDefaultValueParam())
        }
        return paramBuilder.build()
    }

    context(_: Context)
    fun buildMethod(method: GodotClass.ClassMethod, vararg modifiers: KModifier): FunSpec = buildMethod(
        name = method.name,
        returnType = method.returnValue?.let { typeResolver.resolve(it) } ?: UNIT,
        isVararg = method.isVararg,
        arguments = method.arguments,
        extraModifiers = modifiers.toList(),
        methodKdoc = method.description,
    )

    context(_: Context)
    fun buildMethod(method: BuiltinClass.BuiltinMethod, vararg modifiers: KModifier): FunSpec = buildMethod(
        name = method.name,
        returnType = method.returnType,
        isVararg = method.isVararg,
        arguments = method.arguments,
        extraModifiers = modifiers.toList(),
        methodKdoc = method.description,
    )

    context(_: Context)
    fun generateExtension(method: GodotClass.ClassMethod, receiver: ClassName): FunSpec = buildMethod(method)
        .toBuilder()
        .receiver(receiver)
        .build()
}
