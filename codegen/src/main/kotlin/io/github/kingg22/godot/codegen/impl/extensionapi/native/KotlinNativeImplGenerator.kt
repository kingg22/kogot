package io.github.kingg22.godot.codegen.impl.extensionapi.native

import io.github.kingg22.godot.codegen.impl.extensionapi.CodeImplGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.BodyGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.KNativeStructureGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.NativeBuiltinClassGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.NativeEnumGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.NativeMethodGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.NativeVariantGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.stubs.UtilityFunctionStubGenerator
import io.github.kingg22.godot.codegen.models.extensionapi.EnumDescriptor
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import java.nio.file.Path

/** Generates Kotlin Native implementation bodies (cinterop / GDExtension bindings). */
class KotlinNativeImplGenerator(override val typeResolver: TypeResolver) : CodeImplGenerator.ImplGenerator {
    private val bodyGenerator = BodyGenerator()
    private val methodGenerator = NativeMethodGenerator(typeResolver, bodyGenerator)
    private val builtinClassGenerator = NativeBuiltinClassGenerator(typeResolver, bodyGenerator, methodGenerator)
    private val enumGen = NativeEnumGenerator()
    private val variant = NativeVariantGenerator(typeResolver)
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

        // Nested enums are emitted top-level for Kotlin/Native.
        val nestedEnumsPaths = nestedEnums.map { enumGen.generateFile(it).writeTo(outputDir) }
        yieldAll(nestedEnumsPaths)

        // Builtin missing: Variant
        yield(variant.generateFile(nestedEnums.find { it.name == "Variant.Type" }).writeTo(outputDir))

        val builtinClassesPaths = api.builtinClasses.asSequence()
            .mapNotNull { builtinClassGenerator.generateFile(it) }
            .map { it.writeTo(outputDir) }

        yieldAll(builtinClassesPaths)

        // Builtin nested enums → top-level ParentEnum
        val builtinEnumPaths = api.builtinClasses.asSequence()
            .flatMap { builtinClass ->
                builtinClass.enums.asSequence().mapNotNull { enum ->
                    filterNestedEnums(enum, builtinClass.name, api.classes.map { it.name })
                }
            }
            .map { enumGen.generateFile(it).writeTo(outputDir) }

        yieldAll(builtinEnumPaths)

        // FIXME when BodyGenerator is implemented, change to own implementation and remove usage of stubs
        val utilityFunctionsPaths = utils.generate(api.utilityFunctions).map { it.writeTo(outputDir) }

        yieldAll(utilityFunctionsPaths)

        // val godotClassesPaths = api.classes.asSequence()

        val engineClassEnumPaths = api.classes.asSequence()
            .flatMap { engineClass ->
                engineClass.enums.asSequence().mapNotNull { enum ->
                    filterNestedEnums(enum, engineClass.name, api.classes.map { it.name })
                }
            }
            .map { enumGen.generateFile(it).writeTo(outputDir) }

        yieldAll(engineClassEnumPaths)

        val nativeStructuresPaths = api.nativeStructures.asSequence()
            .map { nativeStructure.generateFile(it).writeTo(outputDir) }

        yieldAll(nativeStructuresPaths)
    }

    private fun filterNestedEnums(
        enum: EnumDescriptor,
        parentClassName: String,
        classesNames: List<String>,
    ): EnumDescriptor? {
        if (parentClassName.endsWith('i') &&
            classesNames.any { it == parentClassName.dropLast(1) }
        ) {
            println(
                "INFO: Skipping nested enum '${enum.name}' for class '$parentClassName' because it's a specialized class.",
            )
            return null
        }
        return enum.copy(name = "$parentClassName.${enum.name}")
    }
}
