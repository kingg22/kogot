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
import io.github.kingg22.godot.codegen.models.extensionapi.MethodDescriptor

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
        className: String,
        extraModifiers: List<KModifier> = emptyList(),
        methodKdoc: String? = null,
        originalReturnType: String? = null,
    ): FunSpec {
        val kotlinName = safeIdentifier(name)
        val builder = FunSpec
            .builder(kotlinName)
            .addModifiers(extraModifiers)
            .returns(returnType)
            .addCode(body.todoBody())
            .addKdocIfPresent(methodKdoc)
            .apply { if (originalReturnType != null) addKdoc("\nOriginal return type: `%L`", originalReturnType) }
            .fixAccidentalOverride(name, returnType)
            .experimentalApiAnnotation(className, name)

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

    context(_: Context)
    fun buildMethod(method: MethodDescriptor, className: String, vararg modifiers: KModifier): FunSpec {
        val (originalReturnType, typeName) = when (method) {
            is BuiltinClass.BuiltinMethod ->
                method.returnType to (method.returnType?.let { typeResolver.resolve(it) } ?: UNIT)

            is GodotClass.ClassMethod ->
                method.returnValue?.let { "${it.type}, meta: ${it.meta}" } to
                    (method.returnValue?.let { typeResolver.resolve(it) } ?: UNIT)

            else -> error("Unknown method type: ${method::class}")
        }

        return buildMethod(
            name = method.name,
            returnType = typeName,
            isVararg = method.isVararg,
            arguments = method.arguments,
            className = className,
            extraModifiers = modifiers.toList(),
            methodKdoc = method.description,
            originalReturnType = originalReturnType,
        )
    }

    context(context: Context)
    private fun FunSpec.Builder.fixAccidentalOverride(name: String, returnType: TypeName): FunSpec.Builder {
        when (name) {
            "to_string" if returnType == context.classNameFor("String", "GodotString") -> {
                println("INFO: renaming toString() → toGodotString() to avoid Any clash")
                return build()
                    .toBuilder("toGodotString")
                    .addKdoc(
                        "\n\nGenerated Note: Original name was `toString`, renamed to avoid conflict with [Any.toString].",
                    )
            }

            else -> return this
        }
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
        if (arg.name != kotlinName) paramBuilder.addKdoc("Original name: `%S`", arg.name)
        paramBuilder.addKdoc(
            "\nOriginal type: `%L`, meta type: `%L`",
            arg.type,
            arg.meta ?: "--",
        )
        arg.defaultValue?.let { value ->
            // FIXME remove kdoc when is implemented
            paramBuilder.addKdoc("\nDefault value: `%L`", value)
            paramBuilder.defaultValue(body.todoDefaultValueParam())
        }
        return paramBuilder
            .addKdocForBitfield(arg.type)
            .build()
    }

    context(_: Context)
    fun generateExtension(
        method: GodotClass.ClassMethod,
        receiver: ClassName,
        className: String = receiver.simpleName,
        vararg modifiers: KModifier,
    ): FunSpec = buildMethod(method, className, *modifiers)
        .toBuilder()
        .receiver(receiver)
        .build()
}
