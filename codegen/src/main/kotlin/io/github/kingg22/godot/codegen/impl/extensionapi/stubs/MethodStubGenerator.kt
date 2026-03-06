package io.github.kingg22.godot.codegen.impl.extensionapi.stubs

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import io.github.kingg22.godot.codegen.impl.addKdocForBitfield
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.models.extensionapi.MethodArg

/**
 * Generates a single method [com.squareup.kotlinpoet.FunSpec.Builder] for both class and builtin generators.
 *
 * Handles:
 * - Parameter generation (vararg, type mapping)
 * - Accidental JVM overrides (wait → await, toString → toGodotString)
 * - KDoc for bitfield return types
 */
class MethodStubGenerator(private val typeResolver: TypeResolver) {
    context(_: Context)
    fun generate(
        name: String,
        returnType: TypeName,
        returnTypeString: String?,
        isOpen: Boolean,
        arguments: List<MethodArg>,
    ): FunSpec.Builder {
        val safeName = safeIdentifier(name)
        val params = buildParameters(arguments)

        return FunSpec
            .builder(safeName)
            .addParameters(params)
            .returns(returnType)
            .addKdocForBitfield(returnTypeString, "@return")
            .addStatement("TODO()")
            .applyAccidentalOverrideFix(safeName, returnType)
            .apply { if (isOpen) addModifiers(KModifier.OPEN) }
    }

    context(_: Context)
    private fun buildParameters(arguments: List<MethodArg>): List<ParameterSpec> = arguments.mapIndexed { index, arg ->
        val name = methodArgName(arg, index)
        ParameterSpec
            .builder(name, typeResolver.resolve(arg))
            .addKdocForBitfield(arg.type)
            .build()
    }

    private fun methodArgName(arg: MethodArg, index: Int): String {
        val baseName = arg.name.takeIf { it.isNotBlank() } ?: "arg$index"
        return safeIdentifier(baseName)
    }

    /**
     * Fixes methods whose generated name would accidentally override a JVM / Kotlin built-in.
     *
     * Current fixes:
     * - `wait(): Unit` → renamed to `await` (clashes with [Object.wait])
     * - `toString(): GodotString` → renamed to `toGodotString` (clashes with [Any.toString])
     *
     * Additional clashes can be added here as they are discovered.
     */
    context(context: Context)
    private fun FunSpec.Builder.applyAccidentalOverrideFix(funName: String, returnType: TypeName): FunSpec.Builder =
        when (funName) {
            "wait" if returnType == UNIT -> {
                println("INFO: renaming wait() → await() to avoid JVM Object clash")
                build().toBuilder("await")
                    .addKdoc(
                        "Generated Note: Original name was `wait`, renamed to avoid JVM [java.lang.Object] conflict.",
                    )
            }

            "toString" if returnType == context.classNameForOrDefault("String", "GodotString") -> {
                println("INFO: renaming toString() → toGodotString() to avoid Any clash")
                build().toBuilder("toGodotString")
                    .addKdoc(
                        "Generated Note: Original name was `toString`, renamed to avoid [Any] / [java.lang.Object] conflict.",
                    )
            }

            else -> this
        }
}
