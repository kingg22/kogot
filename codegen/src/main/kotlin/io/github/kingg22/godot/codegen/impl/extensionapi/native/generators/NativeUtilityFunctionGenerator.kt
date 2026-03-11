package io.github.kingg22.godot.codegen.impl.extensionapi.native.generators

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.native.impl.UtilityFunctionImplGen
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.withExceptionContext
import io.github.kingg22.godot.codegen.models.extensionapi.UtilityFunction

/**
 * Generates the `GD` object that exposes Godot API utility functions.
 *
 * When [implGen] is provided (i.e. we're in the native *implementation* backend rather than
 * a stub backend), each generated function receives:
 *
 * 1. A private `lazy(PUBLICATION)` property that resolves the `GDExtensionPtrUtilityFunction`
 *    pointer via `VariantBinding.instance.getPtrUtilityFunctionRaw`.
 * 2. A real function body that invokes the pointer, replacing the default `TODO()` stub.
 *
 * When [implGen] is `null` (stub backends, test harnesses), only the public API shape is
 * generated — all bodies are `TODO()`.
 */
class NativeUtilityFunctionGenerator(
    private val methodGen: NativeMethodGenerator,
    private val implGen: UtilityFunctionImplGen,
) {
    context(context: Context)
    fun generateFile(functions: List<UtilityFunction>): FileSpec {
        val spec = generateSpec(functions)
        return createFile(spec, spec.name!!, context.packageForUtilObject())
    }

    context(context: Context)
    fun generateSpec(functions: List<UtilityFunction>): TypeSpec {
        withExceptionContext({ "Generating utility functions, count: ${functions.size}" }) {
            val functionsSpec = functions.map { fn ->
                withExceptionContext({ "Error generating utility function '${fn.name}'" }) {
                    // Ask implGen for a real body; null → NativeMethodGenerator falls back to TODO().
                    val implBody = implGen.buildFunctionBody(fn)

                    methodGen.buildMethod(
                        method = fn,
                        className = "GD",
                        codeBody = implBody,
                    ) {
                        addKdoc("\n\n**Category**: `%S`", fn.category)
                    }.let { methodSpec ->
                        // FIXME Godot JSON needs to provides all nullable information
                        // Patch return type to nullable for engine class returns
                        if (fn.name == "instance_from_id" && fn.returnType == "Object") {
                            val nonNullReturnType = methodSpec.returnType
                            return@let methodSpec
                                .toBuilder()
                                .returns(nonNullReturnType.copy(nullable = true))
                                .build()
                        }
                        methodSpec
                    }
                }
            }

            val typeBuilder = TypeSpec
                .objectBuilder("GD")
                .addKdoc("Utility functions for Godot API.")

            // Add all lazy function-pointer properties before the public functions so
            // readers see the loading machinery grouped together.
            functions.forEach { fn ->
                withExceptionContext({ "Error generating fn-ptr property for '${fn.name}'" }) {
                    typeBuilder.addProperty(implGen.buildFunctionPointerProperty(fn))
                }
            }

            typeBuilder.addFunctions(functionsSpec)
            return typeBuilder.build()
        }
    }
}
