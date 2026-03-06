package io.github.kingg22.godot.codegen.impl.extensionapi.native.generators

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import io.github.kingg22.godot.codegen.impl.addKdocForBitfield
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.impl.withExceptionContext
import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinClass
import io.github.kingg22.godot.codegen.models.extensionapi.EngineClass
import io.github.kingg22.godot.codegen.models.extensionapi.MethodArg
import io.github.kingg22.godot.codegen.models.extensionapi.MethodDescriptor
import io.github.kingg22.godot.codegen.models.extensionapi.UtilityFunction

/**
 * Shared method/parameter generation logic used by both builtin class
 * generators and engine class generators.
 *
 * Knows nothing about class structure, operators, or statics —
 * those are concerns of the specific generators.
 *
 * @param body provides CodeBlock bodies (stub TODO() or real cinterop calls)
 */
class NativeMethodGenerator(
    private val typeResolver: TypeResolver,
    private val body: BodyGenerator,
    private val defaultValueGenerator: DefaultValueGenerator,
) {

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
        method: MethodDescriptor,
        className: String,
        vararg modifiers: KModifier,
        block: FunSpec.Builder.() -> Unit = {},
    ): FunSpec {
        withExceptionContext({ "Generating method $className.'${method.name}'" }) {
            val (originalReturnType, returnType) = when (method) {
                is BuiltinClass.BuiltinMethod ->
                    method.returnType to (method.returnType?.let { typeResolver.resolve(it) } ?: UNIT)

                is EngineClass.ClassMethod ->
                    method.returnValue?.let { "${it.type}, meta: ${it.meta}" } to
                        (method.returnValue?.let { typeResolver.resolve(it) } ?: UNIT)

                is UtilityFunction -> method.returnType to (method.returnType?.let { typeResolver.resolve(it) } ?: UNIT)
            }

            val name = method.name
            val isVararg = method.isVararg
            val arguments = method.arguments

            val kotlinName = safeIdentifier(name).fixAccidentalOverride(name, returnType)
            val builder = FunSpec
                .builder(kotlinName)
                .addModifiers(*modifiers)
                .returns(returnType)
                .addCode(body.todoBody())
                .addKdocIfPresent(method)
                .apply { if (name != kotlinName) addKdoc("\n\nOriginal name: `%S`", name) }
                .apply { if (originalReturnType != null) addKdoc("\n\nOriginal return type: `%L`", originalReturnType) }
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

            return builder.apply(block).build()
        }
    }

    context(context: Context)
    private fun String.fixAccidentalOverride(godotName: String, returnType: TypeName): String = when (godotName) {
        "to_string" if this == "toString" && returnType == context.classNameFor("String", "GodotString") -> {
            println("INFO: renaming toString() → toGodotString() to avoid Any clash")

            "toGodotString"
        }

        else -> this
    }

    /**
     * Builds a [ParameterSpec] for a single [MethodArg].
     *
     * Default values from Godot JSON are emitted as `TODO()` —
     * the impl layer replaces them with actual expressions.
     */
    context(context: Context)
    fun buildParameter(arg: MethodArg): ParameterSpec {
        withExceptionContext({ "Generating parameter '${arg.name}': ${arg.type} = ${arg.defaultValue ?: "--"}" }) {
            val isNullable = arg.type != "Variant" && arg.defaultValue?.equals("null") ?: false
            val rawType = typeResolver.resolve(arg)
            val type = if (isNullable) rawType.copy(nullable = true) else rawType
            val kotlinName = safeIdentifier(arg.name)
            val paramBuilder = ParameterSpec.builder(kotlinName, type)
            // val isConstructor = methodName == CONSTRUCTOR_NAME

            // Documentación
            if (arg.name != kotlinName) {
                paramBuilder.addKdoc("Original name: `%S`", arg.name)
            }
            paramBuilder.addKdoc(
                "\nOriginal type: `%L`, meta type: `%L`",
                arg.type,
                arg.meta ?: "--",
            )

            // Default value
            arg.defaultValue?.let { value ->
                val defaultCode = defaultValueGenerator.generate(arg, type)
                paramBuilder.addKdoc("\nDefault value (unparsed): `%L`", value)

                @Suppress("VerboseNullabilityAndEmptiness")
                if (defaultCode != null && defaultCode.isNotEmpty()) {
                    paramBuilder.defaultValue(defaultCode)
                } else {
                    // Fallback si no se puede parsear
                    paramBuilder.defaultValue(body.todoDefaultValueParam())
                }
            }

            return paramBuilder
                .addKdocForBitfield(arg.type)
                .build()
        }
    }
}
