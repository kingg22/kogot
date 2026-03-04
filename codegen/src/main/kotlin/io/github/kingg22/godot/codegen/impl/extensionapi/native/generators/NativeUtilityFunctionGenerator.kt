package io.github.kingg22.godot.codegen.impl.extensionapi.native.generators

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.withExceptionContext
import io.github.kingg22.godot.codegen.models.extensionapi.UtilityFunction

/** Generates the `GD` object containing Godot utility functions. */
class NativeUtilityFunctionGenerator(
    private val typeResolver: TypeResolver,
    private val methodGen: NativeMethodGenerator,
) {
    context(context: Context)
    fun generateFile(functions: List<UtilityFunction>): FileSpec {
        val spec = generateSpec(functions)
        return createFile(spec, spec.name!!, context.packageForUtilObject())
    }

    context(context: Context)
    fun generateSpec(functions: List<UtilityFunction>): TypeSpec {
        withExceptionContext({ "Generating utility functions, count: ${functions.size}" }) {
            val gdTypeSpec = TypeSpec
                .objectBuilder("GD")
                .addKdoc("Utility functions for Godot API.")

            functions.forEach { fn ->
                withExceptionContext({ "Error generating utility function '${fn.name}'" }) {
                    val returnType = fn.returnType?.let { typeResolver.resolve(it) } ?: UNIT
                    val funSpec = methodGen.buildMethod(
                        name = fn.name,
                        isVararg = fn.isVararg,
                        returnType = returnType,
                        arguments = fn.arguments,
                        methodKdoc = fn.description?.plus("\nCategory: ${fn.category}") ?: "Category: ${fn.category}",
                        className = "GD",
                    )
                    gdTypeSpec.addFunction(funSpec)
                }
            }
            return gdTypeSpec.build()
        }
    }
}
