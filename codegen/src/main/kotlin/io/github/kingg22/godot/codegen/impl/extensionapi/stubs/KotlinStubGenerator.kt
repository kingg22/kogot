package io.github.kingg22.godot.codegen.impl.extensionapi.stubs

import io.github.kingg22.godot.codegen.impl.extensionapi.CodeImplGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.extensionapi.shared.VariantGenerator
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import java.nio.file.Path

/**
 * Generates Kotlin stubs for the full Godot extension API.
 *
 * Delegates each entity type to a focused sub-generator, keeping this class
 * as an orchestrator only (no generation logic lives here).
 */
class KotlinStubGenerator(override val typeResolver: TypeResolver, packageName: String) :
    CodeImplGenerator.StubGenerator {
    private val enumGen = EnumStubGenerator(packageName)
    private val classGen = ClassStubGenerator(packageName, typeResolver)
    private val builtinGen = BuiltinStubGenerator(packageName, typeResolver, enumGen)
    private val nativeGen = NativeStructureStubGenerator(packageName)
    private val utilityGen = UtilityFunctionStubGenerator(packageName, typeResolver)
    private val variantGen = VariantGenerator(packageName, enumGen, typeResolver)

    init {
        println("WARNING: Kotlin stub generator doesn't generate functional bodies, all throw TODO()")
    }

    context(_: Context)
    override fun generate(api: ExtensionApi, outputDir: Path): Sequence<Path> {
        val paths = mutableListOf<Path>()

        val builtinClassesPaths = api.builtinClasses
            .mapNotNull { builtinGen.generate(it) }
            .map { it.writeTo(outputDir) }
        paths.addAll(builtinClassesPaths)

        val godotClassesPaths = api.classes
            .map { classGen.generate(it) }
            .map { it.writeTo(outputDir) }
        paths.addAll(godotClassesPaths)

        val (nestedEnums, globalEnums) = api.globalEnums.partition { it.name.contains(".") }

        if (nestedEnums.size > 2) {
            System.err.println(
                "WARNING: Nested enums (${nestedEnums.size}) [${nestedEnums.joinToString(postfix = "]") { it.name }}",
            )
        }

        val globalEnumsPaths = globalEnums.map { enumGen.generateFile(it).writeTo(outputDir) }
        paths.addAll(globalEnumsPaths)

        paths.add(variantGen.generate(nestedEnums).writeTo(outputDir))
        paths.add(utilityGen.generate(api.utilityFunctions).writeTo(outputDir))

        val nativeStructuresPaths = api.nativeStructures.map { nativeGen.generate(it).writeTo(outputDir) }
        paths.addAll(nativeStructuresPaths)

        if (api.globalConstants.isNotEmpty()) {
            System.err.println(
                "WARNING: Global constants not supported yet. Found: [${api.globalConstants.joinToString()}]",
            )
        }

        return paths.asSequence()
    }
}
