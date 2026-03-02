package io.github.kingg22.godot.codegen.impl.extensionapi.native

import io.github.kingg22.godot.codegen.impl.extensionapi.CodeImplGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.extensionapi.shared.VariantGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.stubs.EnumStubGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.stubs.UtilityFunctionStubGenerator
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import java.nio.file.Path

/** Generates Kotlin Native implementation bodies (cinterop / GDExtension bindings). */
class KotlinNativeImplGenerator(override val typeResolver: TypeResolver, packageName: String) :
    CodeImplGenerator.ImplGenerator {
    private val builtinClassGenerator = KotlinNativeBuiltinClassGenerator(packageName, typeResolver)
    private val variant = VariantGenerator(EnumStubGenerator(packageName), typeResolver)
    private val utils = UtilityFunctionStubGenerator(packageName, typeResolver)

    context(context: Context)
    override fun generate(api: ExtensionApi, outputDir: Path): Sequence<Path> = sequence {
        if (api.globalConstants.isNotEmpty()) {
            System.err.println(
                "WARNING: Global constants not supported yet. Found: [${api.globalConstants.joinToString()}]",
            )
        }

        // TODO: generate cinterop-based implementations for classes, enums, utility functions
        val (nestedEnums) = api.globalEnums.partition { it.name.contains(".") }

        if (nestedEnums.size > 2) {
            System.err.println(
                "WARNING: Nested enums (${nestedEnums.size}) [${nestedEnums.joinToString(postfix = "]") { it.name }}",
            )
        }

        // Builtin missing: Variant
        yield(variant.generate(nestedEnums).writeTo(outputDir))

        val builtinClassesPaths = api.builtinClasses.asSequence()
            .mapNotNull { builtinClassGenerator.generateFile(it) }
            .map { it.writeTo(outputDir) }

        yieldAll(builtinClassesPaths)

        val utilityFunctionsPaths = utils.generate(api.utilityFunctions).map { it.writeTo(outputDir) }

        yieldAll(utilityFunctionsPaths)
    }
}
