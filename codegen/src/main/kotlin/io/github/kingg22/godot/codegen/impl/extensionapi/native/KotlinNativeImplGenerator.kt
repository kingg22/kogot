package io.github.kingg22.godot.codegen.impl.extensionapi.native

import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.impl.extensionapi.CodeImplGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.extensionapi.stubs.EnumStubGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.stubs.VariantStubGenerator
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import java.nio.file.Path

/**
 * Generates Kotlin Native implementation bodies (cinterop / GDExtension bindings).
 *
 * This is a placeholder — fill in as you implement each entity type.
 */
class KotlinNativeImplGenerator(override val typeResolver: TypeResolver, private val packageName: String) :
    CodeImplGenerator.ImplGenerator {
    private val builtinClassGenerator = KotlinNativeBuiltinClassGenerator(packageName, typeResolver)
    private val variant = VariantStubGenerator(packageName, EnumStubGenerator(packageName), typeResolver)

    context(_: Context)
    override fun generate(api: ExtensionApi, outputDir: Path): Sequence<Path> = sequence {
        // TODO: generate cinterop-based implementations for classes, builtins, utility functions
        yield(variant.generate(emptyList()).writeTo(outputDir))
        val builtinClassesPaths = api.builtinClasses.asSequence()
            .mapNotNull { builtinClassGenerator.generate(it) }
            .map { createFile(it, it.name!!, packageName).writeTo(outputDir) }
        yieldAll(builtinClassesPaths)
    }
}
