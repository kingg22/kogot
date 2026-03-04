package io.github.kingg22.godot.codegen.impl.extensionapi.native

import io.github.kingg22.godot.codegen.impl.extensionapi.CodeImplGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.BodyGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.KNativeStructureGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.NativeBuiltinClassGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.NativeEngineClassGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.NativeEnumGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.NativeMethodGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.NativeVariantGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.stubs.UtilityFunctionStubGenerator
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import java.nio.file.Path

/** Generates Kotlin Native implementation bodies (cinterop / GDExtension bindings). */
class KotlinNativeImplGenerator(override val typeResolver: TypeResolver) : CodeImplGenerator.ImplGenerator {
    private val bodyGenerator = BodyGenerator()
    private val methodGenerator = NativeMethodGenerator(typeResolver, bodyGenerator)
    private val enumGen = NativeEnumGenerator()
    private val builtinClass = NativeBuiltinClassGenerator(typeResolver, bodyGenerator, methodGenerator, enumGen)
    private val engineClass = NativeEngineClassGenerator(typeResolver, bodyGenerator, methodGenerator, enumGen)
    private val variant = NativeVariantGenerator(typeResolver, enumGen)
    private val nativeStructure = KNativeStructureGenerator(typeResolver, bodyGenerator)
    private val utils = UtilityFunctionStubGenerator(typeResolver)

    context(context: Context)
    override fun generate(api: ExtensionApi, outputDir: Path): Sequence<Path> = sequence {
        if (api.globalConstants.isNotEmpty()) {
            System.err.println(
                "WARNING: Global constants not supported yet. Found: [${api.globalConstants.joinToString()}]",
            )
        }

        val (nestedEnums, globalEnums) = api.globalEnums.partition { it.name.contains(".") }

        if (nestedEnums.size > 2) {
            println(
                "WARNING: Nested enums (${nestedEnums.size}) [${nestedEnums.joinToString(postfix = "]") { it.name }}",
            )
        }

        val globalEnumsPaths = globalEnums.map { enumGen.generateFile(it).writeTo(outputDir) }
        yieldAll(globalEnumsPaths)

        // Builtin missing: Variant
        yield(variant.generateFile(nestedEnums).writeTo(outputDir))

        val builtinClassesPaths = api.builtinClasses.asSequence()
            .mapNotNull { builtinClass.generateFile(it) }
            .map { it.writeTo(outputDir) }

        yieldAll(builtinClassesPaths)

        // FIXME when BodyGenerator is implemented, change to own implementation and remove usage of stubs
        val utilityFunctionsPaths = utils.generate(api.utilityFunctions).map { it.writeTo(outputDir) }

        yieldAll(utilityFunctionsPaths)

        val godotClassesPaths = api.classes.asSequence().map { engineClass.generateFile(it).writeTo(outputDir) }

        yieldAll(godotClassesPaths)

        val nativeStructuresPaths = api.nativeStructures.asSequence().map {
            nativeStructure.generateFile(it).writeTo(outputDir)
        }

        yieldAll(nativeStructuresPaths)
    }
}
