package io.github.kingg22.godot.codegen.impl.extensionapi.stubs

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import io.github.kingg22.godot.codegen.impl.commonConfiguration
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.withExceptionContext
import io.github.kingg22.godot.codegen.models.extensionapi.UtilityFunction

/**
 * Generates the `GD` object containing Godot utility functions.
 */
class UtilityFunctionStubGenerator(
    private val typeResolver: TypeResolver,
    private val methodGen: MethodStubGenerator = MethodStubGenerator(typeResolver),
) {

    context(context: Context)
    fun generate(functions: List<UtilityFunction>): List<FileSpec> = withExceptionContext({
        "Generating utility functions, count: ${functions.size}"
    }) {
        val pkg = context.packageForUtilObject()
        val utilsPkg = context.packageForUtilityFun()

        val gdTypeName = ClassName(pkg, "GD")
        val gdTypeSpec = TypeSpec
            .objectBuilder(gdTypeName)
            .addKdoc(
                """
                Utility functions for Godot API.
                To avoid clash definition with builtin kotlin functions, all utility functions are extension function located in _utils_ package.
                """.trimIndent(),
            )
            .build()

        functions
            .groupBy { it.category }
            .map { (category, funs) ->
                val typeBuilder = FileSpec
                    .builder(utilsPkg, "Utils.$category")
                    .addFileComment("Utility functions of $category for Godot API.")
                    .commonConfiguration()

                funs.forEach { fn ->
                    withExceptionContext({ "Error generating utility function '${fn.name}'" }) {
                        val returnType = fn.returnType?.let { typeResolver.resolve(it) } ?: UNIT
                        val funSpec = methodGen.generate(
                            name = fn.name,
                            returnType = returnType,
                            returnTypeString = fn.returnType,
                            isOpen = false,
                            arguments = fn.arguments,
                        )
                            .receiver(gdTypeName)
                            .addKdoc("Category: %L", fn.category)
                            .build()
                        typeBuilder.addFunction(funSpec)
                    }
                }
                typeBuilder.build()
            } + createFile(gdTypeSpec, "GD", pkg)
    }
}
