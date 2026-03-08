package io.github.kingg22.godot.codegen.impl.extensionapi.native

import com.squareup.kotlinpoet.FileSpec
import io.github.kingg22.godot.codegen.impl.extensionapi.CodeImplGenerator
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.extensionapi.native.generators.*
import io.github.kingg22.godot.codegen.impl.extensionapi.native.impl.BuiltinClassImplGen
import io.github.kingg22.godot.codegen.impl.extensionapi.native.impl.ImplementationPackageRegistry
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi

/** Generates Kotlin Native implementation bodies (cinterop / GDExtension bindings). */
class KotlinNativeImplGenerator(override val typeResolver: TypeResolver) : CodeImplGenerator.ImplGenerator {
    private lateinit var implPackageRegistry: ImplementationPackageRegistry
    private val bodyGenerator = BodyGenerator()
    private val builtinClassImplGen = BuiltinClassImplGen(bodyGenerator)
    private val defaultValue = DefaultValueGenerator(typeResolver)
    private val methodGenerator = NativeMethodGenerator(typeResolver, bodyGenerator, defaultValue)
    private val genericInterceptor = GenericBuiltinInterceptor(typeResolver)
    private val enumGen = NativeEnumGenerator()
    private val typeAliasGen = TypeAliasGenerator(genericInterceptor)
    private val builtinClass = NativeBuiltinClassGenerator(
        typeResolver,
        builtinClassImplGen,
        defaultValue,
        methodGenerator,
        enumGen,
        genericInterceptor,
        typeAliasGen,
    )
    private val engineClass = NativeEngineClassGenerator(typeResolver, bodyGenerator, methodGenerator, enumGen)
    private val variant = NativeVariantGenerator(typeResolver, enumGen)
    private val nativeStructure = KNativeStructureGenerator(typeResolver, bodyGenerator)
    private val utils = NativeUtilityFunctionGenerator(methodGenerator)

    context(context: Context)
    private fun initializeImplPackageRegistry() {
        implPackageRegistry = ImplementationPackageRegistry(
            context.rootPackage,
            checkNotNull(context.extensionInterface),
        )
        builtinClassImplGen.initialize(implPackageRegistry)
        nativeStructure.initialize(implPackageRegistry)
    }

    context(context: Context)
    override fun generate(api: ExtensionApi): Sequence<FileSpec> = sequence {
        initializeImplPackageRegistry()

        val builtinClassesPaths = context.model.builtins.asSequence().mapNotNull {
            builtinClass.generateFile(it)
        }

        yieldAll(builtinClassesPaths)

        yield(utils.generateFile(api.utilityFunctions))

        val nativeStructuresPaths = context.model.nativeStructures.asSequence().mapNotNull {
            nativeStructure.generateFile(it)
        }

        yieldAll(nativeStructuresPaths)

        val godotClassesPaths = context.model.engineClasses.asSequence().map {
            engineClass.generateFile(it)
        }

        yieldAll(godotClassesPaths)

        val (nestedEnums, globalEnums) = context.model.globalEnums.partition { it.ownerName != null }

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
