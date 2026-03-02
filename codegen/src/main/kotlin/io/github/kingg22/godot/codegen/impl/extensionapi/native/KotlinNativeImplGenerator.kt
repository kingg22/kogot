package io.github.kingg22.godot.codegen.impl.extensionapi.native

import io.github.kingg22.godot.codegen.impl.extensionapi.CodeImplGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.NativeBuiltinClassGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.NativeEnumGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.NativeVariantGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.stubs.UtilityFunctionStubGenerator
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import java.nio.file.Path

/** Generates Kotlin Native implementation bodies (cinterop / GDExtension bindings). */
class KotlinNativeImplGenerator(
    override val typeResolver: TypeResolver,
    private val builtinClassGenerator: NativeBuiltinClassGenerator = NativeBuiltinClassGenerator(typeResolver),
    private val enumGen: NativeEnumGenerator = NativeEnumGenerator(),
    private val variant: NativeVariantGenerator = NativeVariantGenerator(typeResolver),
    private val utils: UtilityFunctionStubGenerator = UtilityFunctionStubGenerator(typeResolver),
) : CodeImplGenerator.ImplGenerator {

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
                    if (builtinClass.name.endsWith('i') &&
                        api.builtinClasses.any { it.name == builtinClass.name.dropLast(1) }
                    ) {
                        println(
                            "WARNING: Skipping nested enum '${enum.name}' for builtin class '${builtinClass.name}' because it's a specialized class.",
                        )
                        return@mapNotNull null
                    }
                    enum.copy(name = "${builtinClass.name}.${enum.name}")
                }
            }
            .map { enumGen.generateFile(it).writeTo(outputDir) }

        yieldAll(builtinEnumPaths)

        val utilityFunctionsPaths = utils.generate(api.utilityFunctions).map { it.writeTo(outputDir) }

        yieldAll(utilityFunctionsPaths)
    }
}
