package io.github.kingg22.godot.codegen.impl.extensionapi.native.generators

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.withExceptionContext
import io.github.kingg22.godot.codegen.models.extensionapi.UtilityFunction

/** Generates the `GD` object containing Godot utility functions. */
class NativeUtilityFunctionGenerator(private val methodGen: NativeMethodGenerator) {

    context(context: Context)
    fun generateFile(functions: List<UtilityFunction>): FileSpec {
        val spec = generateSpec(functions)
        return createFile(spec, spec.name!!, context.packageForUtilObject())
    }

    context(_: Context)
    fun generateSpec(functions: List<UtilityFunction>): TypeSpec {
        withExceptionContext({ "Generating utility functions, count: ${functions.size}" }) {
            val functionsSpec = functions.map { fn ->
                withExceptionContext({ "Error generating utility function '${fn.name}'" }) {
                    methodGen.buildMethod(method = fn, className = "GD") {
                        addKdoc("\n\n**Category**: `%S`", fn.category)
                    }
                }
            }

            return TypeSpec
                .objectBuilder("GD")
                .addKdoc("Utility functions for Godot API.")
                .addFunctions(functionsSpec)
                .build()
        }
    }
}
