package io.github.kingg22.godot.codegen.impl.extensionapi.native

import com.squareup.kotlinpoet.FileSpec
import io.github.kingg22.godot.codegen.impl.extensionapi.CodeImplGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.BodyGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.DefaultValueGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.KNativeStructureGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.NativeBuiltinClassGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.NativeEngineClassGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.NativeEnumGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.NativeMethodGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.NativeUtilityFunctionGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.NativeVariantGenerator
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi

/** Generates Kotlin Native implementation bodies (cinterop / GDExtension bindings). */
class KotlinNativeImplGenerator(override val typeResolver: TypeResolver) : CodeImplGenerator.ImplGenerator {
    private val bodyGenerator = BodyGenerator()
    private val defaultValue = DefaultValueGenerator(typeResolver)
    private val methodGenerator = NativeMethodGenerator(typeResolver, bodyGenerator, defaultValue)
    private val enumGen = NativeEnumGenerator()
    private val builtinClass = NativeBuiltinClassGenerator(
        typeResolver,
        bodyGenerator,
        defaultValue,
        methodGenerator,
        enumGen,
    )
    private val engineClass = NativeEngineClassGenerator(typeResolver, bodyGenerator, methodGenerator, enumGen)
    private val variant = NativeVariantGenerator(typeResolver, enumGen)
    private val nativeStructure = KNativeStructureGenerator(typeResolver, bodyGenerator)
    private val utils = NativeUtilityFunctionGenerator(methodGenerator)

    context(context: Context)
    override fun generate(api: ExtensionApi): Sequence<FileSpec> = sequence {
        val builtinClassesPaths = api.builtinClasses.asSequence().mapNotNull {
            builtinClass.generateFile(it)
        }

        yieldAll(builtinClassesPaths)

        yield(utils.generateFile(api.utilityFunctions))

        val nativeStructuresPaths = api.nativeStructures.asSequence().mapNotNull {
            nativeStructure.generateFile(it)
        }

        yieldAll(nativeStructuresPaths)

        val godotClassesPaths = api.classes.asSequence().map {
            engineClass.generateFile(it)
        }

        yieldAll(godotClassesPaths)

        val (nestedEnums, globalEnums) = api.globalEnums.partition { it.name.contains(".") }

        val globalEnumsPaths = globalEnums.asSequence().map {
            enumGen.generateFile(it)
        }
        yieldAll(globalEnumsPaths)

        if (nestedEnums.size > 2) {
            println(
                "WARNING: Nested enums (${nestedEnums.size}) [${nestedEnums.joinToString(postfix = "]") { it.name }}",
            )
        }

        // Builtin missing: Variant
        yield(variant.generateFile(nestedEnums))

        if (api.globalConstants.isNotEmpty()) {
            System.err.println(
                "WARNING: Global constants not supported yet. Found: [${api.globalConstants.joinToString()}]",
            )
        }
    }
}
